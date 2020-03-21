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
import org.stacktrace.yo.ombi.OmbiAPI.{OmbiMovieSearchResult, OmbiParams, OmbiTVSearchResult}
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import sttp.client.{Identity, Response, ResponseError}

import scala.collection.mutable
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future, Promise}
import scala.language.postfixOps
import scala.util.Try


class OmbiBot(val token: String)(implicit val ombi: OmbiParams) extends TelegramBot with Polling with Callbacks[Future] with Commands[Future] {

  type Searcher[A] = String => Identity[Response[Either[ResponseError[Exception], Seq[A]]]]
  type ResultMapper[A] = Seq[A] => (Option[Seq[AvailMediaData]], Option[Seq[MediaRequestData]])
  type MessageMaker = Option[Seq[MediaRequestData]] => Option[Seq[SendMessage]]
  type AvailMessageMaker = Option[Seq[AvailMediaData]] => Option[SendMessage]
  type MediaRequester = String => Identity[Response[Either[String, String]]]

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE
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

     - Note: Both search commands work with imdb urls

       Example: */searchtv https://www.imdb.com/title/tt6751668*
    """

  private val chatState: mutable.Map[Long, Seq[Int]] = collection.mutable.Map[Long, Seq[Int]]()
  override val client: RequestHandler[Future] = new ScalajHttpClient(token)

  scheduleClear()

  onCommand(SEARCH_TV) { implicit msg =>
    runCommand(OmbiAPI.searchTV, tvResultsMessage, tvResultsMarkup, tvAvailReply)
  }

  onCommand(SEARCH_ALL) { implicit msg =>
    sequentialSendAndSaveMessageId(
      Seq(SendMessage(msg.source, "*Searching Movies and TV*", parseMode = Some(ParseMode.Markdown), disableNotification = Some(true)))
    ).map(ids => chatState(msg.source) = ids)
      .flatMap(_ => runCommand(OmbiAPI.searchMovie, movieResultsMessage, movieResultsMarkup, movieAvailReply)
        .flatMap(_ => runCommand(OmbiAPI.searchTV, tvResultsMessage, tvResultsMarkup, tvAvailReply)))

  }

  onCommand(SEARCH_MOVIE) { implicit msg =>
    runCommand(OmbiAPI.searchMovie, movieResultsMessage, movieResultsMarkup, movieAvailReply)
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
        request(textMD("*IMDB link detected...*")).map(im =>
          after(duration = 2 seconds) {
            request(DeleteMessage(ChatId(im.source), im.messageId))
          })
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
      val lr = requester(data).body.fold(_ => textMD("*Error Requesting*")(msg), _ => textMD("*Successfully Requested*")(msg))
      runDeleteAll()
      request(lr).map(resMsg => {
        after(duration = 5 seconds) {
          request(DeleteMessage(ChatId(resMsg.source), resMsg.messageId))
        }
      })
    })
      .getOrElse({
        runDeleteAll()
        request(textMD("Hm something went wrong")(msg)).map(resMsg => {
          after(duration = 5 seconds) {
            request(DeleteMessage(ChatId(resMsg.source), resMsg.messageId))
          }
        })
      }
      ).void
  }

  def scheduleClear(d: Duration = 5 minutes): Unit = {

    val t = new Timer()
    t.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = {
        chatState.foreach(a => a._2.map(id => DeleteMessage(ChatId(a._1), id)).map(request(_)))
        chatState.clear()
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

  private val tmdbLink: String => String = (id: String) => s"https://www.themoviedb.org/movie/${id}"

  private val tvdbLink: String => String = (id: String) => s"https://www.thetvdb.com/?id=${id}&tab=series"

  private val movieTag: String => String = prefixTag(M_TAG)

  private val tvTag: String => String = prefixTag(T_TAG)

  case class MediaRequestData(link: String, requestName: String, requestId: String)

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
      .map(r => MediaRequestData(s"${tmdbLink(r.theMovieDbId)}", s"${r.title} ${convertStringToDate(r.releaseDate, iso = true)}", r.theMovieDbId))

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
      .map(r => MediaRequestData(s"${tvdbLink(r.infoId)}", s"${r.title} ${convertStringToDate(r.firstAired)}", r.theTvDbId))

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
}

