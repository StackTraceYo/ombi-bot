package org.stacktrace.yo.ombi


import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.{Timer, TimerTask}

import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.{Callbacks, Commands}
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods.{DeleteMessage, ParseMode, SendMessage}
import com.bot4s.telegram.models._
import org.stacktrace.yo.ombi.ConfigLoader.BotConfig
import org.stacktrace.yo.ombi.OmbiAPI.{OmbiMovieSearchResult, OmbiParams, OmbiTVSearchResult}
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import sttp.client.{Identity, Response, ResponseError}

import scala.collection.mutable
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future, Promise}
import scala.io.Source
import scala.language.postfixOps
import scala.util.Try


class OmbiBot(val config: BotConfig) extends TelegramBot with Polling with Callbacks[Future] with Commands[Future] with BotAuthorization {

  implicit val ombi: OmbiParams = config.ombi
  private val token: String = config.botToken
  private val push: Option[Long] = Option.empty

  val name: String = config.botName

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE


  type Searcher[A] = String => Identity[Response[Either[ResponseError[Exception], Seq[A]]]]
  type ResultMapper[A] = Seq[A] => (Option[Seq[AvailMediaData]], Option[Seq[MediaRequestData]])
  type MessageMaker = Option[Seq[MediaRequestData]] => Option[Seq[SendMessage]]
  type AvailMessageMaker = Option[Seq[AvailMediaData]] => Option[SendMessage]
  type MediaRequester = String => Identity[Response[Either[String, String]]]


  private val M = 0
  private val T = 0
  private val M_TAG = "M_TAG"
  private val T_TAG = "T_TAG"
  private val SEARCH_TV = "searchtv"
  private val SEARCH_MOVIE = "searchmovie"
  private val SEARCH_ALL = "search"
  private val INFO = "info"
  private val info =
    """
    OMBI Bot Commands:

     - /searchmovie command
       Example: */searchmovie the dark knight*

     - /searchtv command
       Example: */searchtv star wars rebels*

     - /search command (tv and movie search)
       Example: */search batman*

     - /info to see this message
     - /authinfo to see this authorization message

     - Note: Both search commands work with imdb urls

       Example: */searchtv https://www.imdb.com/title/tt6751668*
    """


  private val chatState: mutable.Map[Long, Seq[Int]] = collection.mutable.Map[Long, Seq[Int]]()
  private val requestIdState: mutable.Map[String, MediaRequestData] = collection.mutable.Map[String, MediaRequestData]()
  override val client: RequestHandler[Future] = new ScalajHttpClient(token)

  def tryToLong(s: String): Option[Int] = Try(s.toInt).toOption

  scheduleClear()

  onCommand(SEARCH_TV) { implicit msg =>
    authenticateAndRun {
      () => runCommand(OmbiAPI.searchTV, tvResultsMessage, tvResultsMarkup, tvAvailReply)
    }
  }

  onCommand(SEARCH_ALL) { implicit msg =>
    authenticateAndRun {
      () => {
        sequentialSendAndSaveMessageId(
          Seq(SendMessage(msg.source, "*Searching Movies and TV*", parseMode = Some(ParseMode.Markdown), disableNotification = Some(true)))
        ).map(ids => chatState(msg.source) = ids)
          .flatMap(_ => runCommand(OmbiAPI.searchMovie, movieResultsMessage, movieResultsMarkup, movieAvailReply)
            .flatMap(_ => runCommand(OmbiAPI.searchTV, tvResultsMessage, tvResultsMarkup, tvAvailReply)))
      }
    }

  }

  onCommand(SEARCH_MOVIE) { implicit msg =>
    authenticateAndRun {
      () => runCommand(OmbiAPI.searchMovie, movieResultsMessage, movieResultsMarkup, movieAvailReply)
    }
  }

  onCommand(INFO) { implicit msg =>
    replyMd(info, disableWebPagePreview = Some(true)).void
  }


  onCallbackWithTag(T_TAG) { implicit cbq =>
    callback(cbq, OmbiAPI.requestTV)
  }

  onCallbackWithTag(M_TAG) { implicit cbq =>
    callback(cbq, OmbiAPI.requestMovie)
  }

  private def noAvail(implicit msg: Message): SendMessage = textMD("*No Available Results*")

  private def noRes(implicit msg: Message): SendMessage = textMD("*No Search Results*")

  private def noResNoAvail(implicit msg: Message): (SendMessage, (SendMessage, Seq[SendMessage])) = (noAvail, (noRes, Seq()))

  def runCommand[A](searcher: Searcher[A], resulted: ResultMapper[A], marker: MessageMaker, availMaker: AvailMessageMaker)(implicit msg: Message): Future[Unit] = {
    withArgs { args =>
      val query = args.mkString(" ")
      val parsed = if (IMDBSearch.looksLike(query)) {
        deleteAfter(request(textMD("*IMDB link detected...*")), 2 seconds)
        IMDBSearch(query)
      } else {
        query
      }
      val (avail, (resultsHeader, resMessages)) = searcher(parsed).body.fold(
        _ => noResNoAvail,
        r =>
          if (r.isEmpty) {
            noResNoAvail
          }
          else {
            val (avail, results) = resulted(r)
            val lRes = defaultMessage(availMaker(avail), msg, "*No results available in Plex*")
            val rRes: (SendMessage, Seq[SendMessage]) = marker(results)
              .map(r => (textMD(s"*Found ${r.length} Search Results*\n\n"), r))
              .getOrElse((noRes, Seq()))
            (lRes, rRes)
          }
      )
      sequentialSendAndSaveMessageId(avail +: (resultsHeader +: resMessages))
        .map(ids => chatState(msg.source) = ids)
        .void
    }
  }

  def callback(cbq: CallbackQuery, requester: MediaRequester): Future[Unit] = {
    val msg = cbq.message.get
    val runDeleteAll = () => Future.sequence(
      chatState.remove(msg.source).getOrElse(Seq())
        .map(d => DeleteMessage(ChatId(msg.source), d)).map(request(_))
    ).void

    cbq.data.map(data => {
      deleteAfter(reply("Requesting...")(msg))
      val media: Option[MediaRequestData] = requestIdState.remove(data)
      val name = media.map(_.requestName).getOrElse("Unknown")
      val lr = requester(data).body.fold(_ => (textMD("*Error Requesting*")(msg), false), _ => (textMD(s"*Successfully Requested $name*")(msg), true))
      runDeleteAll()
      request(lr._1).map(_ => {
        val pushText = media.map(m => {
          s"Received Request for ${m.requestName} ${imdbLink(m.imdb)}"
        }).getOrElse(s"*Request for $name received*")
        after(duration = 5 seconds) {
          if (lr._2) {
            push.foreach(p => {
              request(SendMessage(ChatId(p), pushText, parseMode = Some(ParseMode.Markdown), disableWebPagePreview = Some(true)))
            })
          }
        }
      })
    })
      .getOrElse({
        runDeleteAll()
        deleteAfter(request(textMD("Hm something went wrong")(msg)))
      }
      ).void
  }

  def scheduleClear(d: Duration = 5 minutes): Unit = {

    val t = new Timer()
    t.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = {
        chatState.foreach(a => a._2.map(id => DeleteMessage(ChatId(a._1), id)).map(request(_)))
        chatState.clear()
        requestIdState.clear()
      }
    }, 0, d.toMillis)
  }

  def convertStringToDate(s: String, iso: Boolean = false): String = {
    try {
      if (s.isEmpty) {
        ""
      } else {
        val year = if (iso) {
          LocalDate.parse(s, DateTimeFormatter.ISO_DATE_TIME).getYear
        } else {
          LocalDate.parse(s).getYear
        }
        "(" + year + ")"
      }
    } catch {
      case e: Throwable => ""
    }
  }

  def optionIfEmpty[A](seq: Seq[A]): Option[Seq[A]] = {
    if (seq.isEmpty) {
      Option.empty
    } else {
      Some(seq)
    }
  }

  private val tmdbLink: String => String = (id: String) => s"https://www.themoviedb.org/movie/$id"

  private val tvdbLink: String => String = (id: String) => s"https://www.thetvdb.com/?id=$id&tab=series"

  private val imdbLink: String => String = (id: String) => s"https://www.imdb.com/title/$id"


  private val movieTag: String => String = prefixTag(M_TAG)

  private val tvTag: String => String = prefixTag(T_TAG)

  case class MediaRequestData(link: String, requestName: String, requestId: String, imdb: String, t: Int)

  case class AvailMediaData(link: String, plexLink: String)

  def movieResultsMessage(results: Seq[OmbiMovieSearchResult]): (Option[Seq[AvailMediaData]], Option[Seq[MediaRequestData]]) = {
    val available = results.take(math.min(results.length, 5))
      .filter(_.isAvailable)
      .map(r => {
        val releaseDate = convertStringToDate(r.releaseDate, iso = true)
        AvailMediaData(s"[${r.title} $releaseDate](${tmdbLink(r.theMovieDbId)})", s"[Plex Link](${r.plexUrl})")
      })

    val canRequest = results.take(math.min(results.length, 5))
      .filter(!_.isAvailable)
      .map(r => MediaRequestData(s"${tmdbLink(r.theMovieDbId)}", s"${r.title} ${convertStringToDate(r.releaseDate, iso = true)}", r.theMovieDbId, r.imdbId, M))

    canRequest.foreach(a => requestIdState.put(a.requestId, a))
    (optionIfEmpty(available), optionIfEmpty(canRequest))
  }

  def tvResultsMessage(results: Seq[OmbiTVSearchResult]): (Option[Seq[AvailMediaData]], Option[Seq[MediaRequestData]]) = {
    val available = results.take(math.min(results.length, 5))
      .filter(_.isAvailable)
      .map(r => {
        val releaseDate = convertStringToDate(r.firstAired)
        AvailMediaData(s"[${r.title} $releaseDate](${tvdbLink(r.infoId)})", s"[Plex Link](${r.plexUrl})")
      })

    val canRequest = results.take(math.min(results.length, 5))
      .filter(!_.isAvailable)
      .map(r => MediaRequestData(s"${tvdbLink(r.infoId)}", s"${r.title} ${convertStringToDate(r.firstAired)}", r.theTvDbId, r.imdbId, T))
    canRequest.foreach(a => requestIdState.put(a.requestId, a))
    (optionIfEmpty(available), optionIfEmpty(canRequest))
  }

  def resultsMessageAndMarkup(movie: Boolean)(implicit msg: Message): MessageMaker = {
    results: Option[Seq[MediaRequestData]] => {
      results.map(media => {
        media.map(m => {
          (m.requestName, InlineKeyboardMarkup.singleRow(
            Seq(
              InlineKeyboardButton.url(ifElse(movie, "TMDb", "TVDB"), m.link),
              InlineKeyboardButton.callbackData("Request", ifElse(movie, movieTag, tvTag)(m.requestId))
            )
          ))
        }).map(pair => SendMessage(msg.source, pair._1, replyMarkup = Some(pair._2)))
      })
    }
  }

  def movieResultsMarkup(implicit msg: Message): MessageMaker = resultsMessageAndMarkup(movie = true)

  def tvResultsMarkup(implicit msg: Message): MessageMaker = resultsMessageAndMarkup(movie = false)

  def textMD(t: String)(implicit msg: Message): SendMessage = {
    SendMessage(msg.source, t, parseMode = Some(ParseMode.Markdown))
  }

  def defaultMessage(msg: Option[SendMessage], a: Message, default: String): SendMessage = {
    msg.getOrElse(textMD(default)(a)).copy(disableWebPagePreview = Some(true))
  }

  def movieAvailReply(implicit msg: Message): AvailMessageMaker = availableReply("movies")

  def tvAvailReply(implicit msg: Message): AvailMessageMaker = availableReply("TV series")

  def availableReply(media: String)(implicit msg: Message): AvailMessageMaker = {
    avail: Option[Seq[AvailMediaData]] => {
      avail.map(a => textMD(s"*Found ${math.min(a.size, 5)} ${media} available in Plex*\n" + a.zipWithIndex.map(s => {
        val (availData: AvailMediaData, idx: Int) = s
        s"*${idx + 1}* - ${availData.link}  - ${availData.plexLink}"
      }).mkString("\n")))
    }
  }

  private def ifElse[A](a: Boolean, t: A, f: A): A = {
    if (a) {
      t
    } else {
      f
    }
  }

  def sequentialSendAndSaveMessageId(messages: Seq[SendMessage])(implicit msg: Message): Future[Seq[Int]] = {
    val fSequence = {
      var fAccum: Future[Seq[Int]] = Future {
        chatState.getOrElse(msg.source, Seq())
      }
      for (item <- messages) {
        fAccum = fAccum flatMap { acc: Seq[Int] => request(item.copy(disableNotification = Some(true))).map(res => res.messageId +: acc) }
      }
      fAccum
    }
    fSequence
  }

  def after[T](duration: Duration)(block: => T): Future[T] = {
    val promise = Promise[T]()
    val t = new Timer()
    t.schedule(new TimerTask {
      override def run(): Unit = {
        promise.complete(Try(block))
      }
    }, duration.toMillis)
    promise.future
  }

  def deleteAfter(initial: Future[Message], duration: Duration = 5 seconds): Future[Unit] = {
    initial.flatMap((msg: Message) => {
      after(duration = duration) {
        request(DeleteMessage(ChatId(msg.source), msg.messageId))
      }
    }).void
  }
}

trait BotAuthorization {
  self: OmbiBot =>

  val chatId: Option[Long] = config.botChatId
  val admin: Option[Int] = config.botAdmin

  private val AUTH_INFO = "authinfo"

  private val AUTHORIZE = "authorize"
  private val UNAUTHORIZE = "unauthorize"
  private val UNAUTHORIZE_ALL = "unauthorizeall"
  private val ENABLE_AUTH = "authon"
  private val DISABLE_AUTH = "authoff"
  private val REGISTER = "register"
  private val UNREGISTER = "unregister"
  private val UNREGISTER_ALL = "unregisterall"

  private val authInfo =
    """
       *If Authorization is enabled* :

       Users will *not* be able to make requests
       until they are added and the chat is registered

       - /register  will register the current chat
       - /unregister will unregister the current chat
       - /unregisterall will unregister all chats

       - /authorize <user id>
       - /unauthorize <user id>
       - /unauthorizeall

       - /authoff  - disable user auth
       - /authon  - enable user auth


       Notes:
       - toggling authorization does not remove authorized users

       Users can get there id from the @userinfobot

  """

  private var authorizationEnabled: Boolean = admin.isDefined
  private val chatAuthorizationEnabled: Boolean = authorizationEnabled || chatId.isDefined
  private val authorizedUsers: mutable.Set[Int] = if (authorizationEnabled) {
    val set = new mutable.HashSet[Int]()
    set.add(admin.get)
    set
  } else {
    null
  }
  private val authorizedChats: mutable.Set[Long] = if (chatAuthorizationEnabled) {
    chatId.map(id => {
      val set = new mutable.HashSet[Long]()
      set.add(id)
      set
    }).getOrElse(new mutable.HashSet[Long]())
  } else {
    null
  }
  private val disableAuth: () => Unit = () => authorizationEnabled = false
  private val enableAuth: () => Unit = () => authorizationEnabled = true

  private def addChatAuth(id: Long): Long = {
    if (chatAuthorizationEnabled) {
      authorizedChats.add(id)
    }
    id
  }

  private def removeChatAuth(id: Long): Long = {
    if (chatAuthorizationEnabled) {
      authorizedChats.remove(id)
    }
    id
  }

  private def addToAuth(id: Int): Int = {
    if (authorizationEnabled) {
      authorizedUsers.add(id)
    }
    id
  }

  private def removeFromAuth(id: Int): Int = {
    if (authorizationEnabled) {
      authorizedUsers.remove(id)
    }
    id
  }

  onCommand(AUTHORIZE) { implicit msg =>
    withArgs { args =>
      authAction(() => {
        args.headOption.flatMap(tryToLong)
          .map(id => replyMd(s"Authorized User ${addToAuth(id)}"))
          .getOrElse(replyMd("Invalid arguments for authorization"))
      })
    }
  }

  onCommand(UNAUTHORIZE) { implicit msg =>
    withArgs { args =>
      authAction(() => {
        args.headOption.flatMap(tryToLong)
          .map(id => replyMd(s"Unauthorized User ${removeFromAuth(id)}"))
          .getOrElse(replyMd("Invalid arguments for authorization"))
      })
    }
  }

  onCommand(UNAUTHORIZE_ALL) { implicit msg =>
    authAction(() => {
      authorizedUsers.clear()
      authorizedUsers.add(admin.get)
      replyMd(s"Authorizations reset")
    })(msg)
  }

  onCommand(REGISTER) { implicit msg =>
    regAction(() => {
      replyMd(s"Registered Chat ${addChatAuth(msg.chat.id)}")
    })
  }

  onCommand(UNREGISTER) { implicit msg =>
    regAction(() => {
      replyMd(s"Unregistered Chat ${removeChatAuth(msg.chat.id)}")
    })
  }

  onCommand(UNREGISTER_ALL) { implicit msg =>
    regAction(() => {
      authorizedChats.clear()
      replyMd(s"Registrations cleared")
    })(msg)
  }

  onCommand(DISABLE_AUTH) { implicit msg =>
    setAuth(on = false)(msg)
  }

  onCommand(ENABLE_AUTH) { implicit msg =>
    setAuth(on = true)(msg)
  }

  onCommand(AUTH_INFO) { implicit msg =>
    replyMd(authInfo, disableWebPagePreview = Some(true)).void
  }

  def authenticateAndRun(run: () => Future[Unit])(implicit msg: Message): Future[Unit] = {
    val chatEnabled = !chatAuthorizationEnabled || authorizedChats.contains(msg.chat.id)
    if (chatEnabled) {
      if (authorizationEnabled) {
        if (msg.from.map(_.id).exists(id => authorizedUsers.contains(id))) {
          run()
        } else {
          deleteAfter(replyMd("You are not not authorized to make requests"))
        }
      } else {
        run()
      }
    } else {
      deleteAfter(replyMd("This chat is not registered to use this bot"))
    }
  }

  private def regAction(action: () => Future[Message])(implicit msg: Message): Future[Unit] = {
    val isAdmin: Boolean = msg.from.map(_.id).exists(id => admin.contains(id))
    val replyMessage = if (isAdmin && chatAuthorizationEnabled) {
      action()
    } else {
      replyMd("You are not authorized to make this request")
    }
    deleteAfter(replyMessage)
  }

  private def authAction(action: () => Future[Message])(implicit msg: Message): Future[Unit] = {
    val chatEnabled = !chatAuthorizationEnabled || authorizedChats.contains(msg.chat.id)
    val replyMessage = if (chatEnabled) {
      val isAdmin: Boolean = msg.from.map(_.id).exists(id => admin.contains(id))
      if (authorizationEnabled || isAdmin) {
        if (isAdmin) {
          action()
        } else {
          replyMd("You are not authorized to make this request")
        }
      } else {
        replyMd("Authorization not enabled")
      }
    } else {
      replyMd("This chat is not registered to use this bot")
    }
    deleteAfter(replyMessage)
  }

  private def setAuth(on: Boolean)(implicit msg: Message): Future[Unit] = {
    val isAdmin: Boolean = msg.from.map(_.id).exists(id => admin.contains(id))
    val replyMessage = if (isAdmin) {
      val txt = if (on) {
        enableAuth()
        "Enabled"
      } else {
        disableAuth()
        "Disabled"
      }
      replyMd(s"Authorization $txt")
    } else {
      if (admin.isDefined) {
        replyMd("You are not authorized to make this request")
      } else {
        replyMd("Authorization support is not available without an admin")
      }
    }
    deleteAfter(replyMessage)
  }


}

object OmbiBotRunner extends App {

  val config = ConfigLoader(args = args)
  val bot = new OmbiBot(config)
  val eol = bot.run()
  Await.result(eol, Duration.Inf)
}


case class CMDLineConfig(file: Option[String] = Option.empty)

object ConfigLoader {

  case class BotConfig(botName: String, botToken: String, botChatId: Option[Long], botAdmin: Option[Int], ombi: OmbiParams)

  def apply(args: Seq[String]): BotConfig = {
    import scala.collection.JavaConverters._

    val parser = new scopt.OptionParser[CMDLineConfig]("ombi-bot") {
      head("ombi-bot", "2.0")

      opt[String]('p', "params")
        .valueName("<param file>")
        .action((f, c) => c.copy(file = Some(f)))
        .text("Parameter is a path to the env file")
    }

    val params = parser.parse(args, CMDLineConfig())
      .map(config => {
        if (config.file.isDefined) {
          val source = Source.fromFile(config.file.get, "utf-8")
          val params = source.getLines()
            .map(line => Seq(line.trim.split("="): _*))
            .filter(line => line.length == 2)
            .map(seq => (seq.head, seq.last))
            .toMap
          source.close()
          params
        } else {
          System.getenv().asScala
        }
      }).getOrElse(System.getenv().asScala)

    val user = params.get("OMBI_USER_NAME")
    val ombiHost = params.getOrElse("OMBI_HOST", throw new IllegalArgumentException("ENV file is missing Missing OMBI_HOST"))
    val ombiKey = params.getOrElse("OMBI_KEY", throw new IllegalArgumentException("ENV file is missing Missing OMBI_KEY"))
    val botToken = params.getOrElse("BOT_TOKEN", params.getOrElse("OMBI_BOT_TOKEN", throw new IllegalArgumentException("ENV file is missing Missing BOT_TOKEN or OMBI_BOT_TOKEN")))
    val botName = params.getOrElse("BOT_NAME", params.getOrElse("OMBI_BOT_NAME", throw new IllegalArgumentException("ENV file is missing Missing BOT_NAME or OMBI_BOT_NAME")))
    val botChatId = params.get("BOT_CHAT_ID").flatMap(s => Try(s.toLong).toOption)
    val adminId = params.get("BOT_ADMIN").flatMap(s => Try(s.toInt).toOption)

    BotConfig(
      botName = botName,
      botToken = botToken,
      botAdmin = adminId,
      botChatId = botChatId,
      ombi = OmbiParams(ombiHost, ombiKey, user)
    )
  }
}
