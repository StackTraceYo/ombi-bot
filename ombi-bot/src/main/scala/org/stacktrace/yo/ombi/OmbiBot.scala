package org.stacktrace.yo.ombi


import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods.ParseMode
import com.bot4s.telegram.models.Message
import org.stacktrace.yo.ombi.OmbiSearch.{OmbiMovieSearchResult, OmbiParams}
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}
import sttp.client._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

/** Generates random values.
 */
class OmbiBot(val token: String) extends TelegramBot with OmbiBroker
  with Polling
  with Commands[Future] {
  LoggerConfig.factory = PrintLoggerFactory()
  // set log level, e.g. to TRACE
  LoggerConfig.level = LogLevel.TRACE

  // Or just the scalaj-http backend
  override val client: RequestHandler[Future] = new ScalajHttpClient(token)

  val rng = new scala.util.Random(System.currentTimeMillis())
  implicit val ombiParams: OmbiParams = OmbiParams("https://server309.andy10gbit.xyz/ombi", "7becfaa6830b40e883d195a3464c3fa1", Some("daymanbpi"))

  import com.bot4s.telegram.methods.SendMessage
  import com.bot4s.telegram.models.{InlineKeyboardButton, InlineKeyboardMarkup}


  def convertStringToDate(s: String): String = {
    if (s.isEmpty) {
      ""
    } else {
      val year = LocalDate.parse(s, DateTimeFormatter.ISO_DATE_TIME).getYear
      "(" + year + ")"
    }
  }

  def optionIfEmpty[A, B](seq: Seq[A], b: B): Option[B] = {
    if (seq.isEmpty) {
      Option.empty
    } else {
      Some(b)
    }
  }

  def resultsMessage(results: Seq[OmbiMovieSearchResult]): (Option[Seq[String]], Option[InlineKeyboardMarkup]) = {
    val available = results.take(math.min(results.length, 5))
      .filter(_.isAvailable)
      .map(r => {
        val releaseDate = convertStringToDate(r.releaseDate)
        s"${r.title} $releaseDate"
      })

    val canRequest = results.take(math.min(results.length, 5))
      .filter(!_.isAvailable)
      .map(r => {
        val releaseDate = convertStringToDate(r.releaseDate)
        val request = InlineKeyboardButton.callbackData(s"${r.title} $releaseDate", "123")
        val tmdb = InlineKeyboardButton.url("TMDB", s"https://www.themoviedb.org/movie/${r.theMovieDbId}")
        Seq(request, tmdb)
      })

    val keyboard = optionIfEmpty(canRequest, InlineKeyboardMarkup(canRequest))
    val availableList = optionIfEmpty(available, available)

    (availableList, keyboard)
  }

  def textMD(msg: Message, t: String): SendMessage = {
    SendMessage(msg.source, t, parseMode = Some(ParseMode.Markdown))
  }

  def noLeft(msg: SendMessage): (Option[SendMessage], SendMessage) = {
    (Option.empty[SendMessage], msg)
  }

  def defaultMessage(msg: Option[SendMessage], a: Message, default: String) = {
    msg.getOrElse(textMD(a, default))
  }

  def availableReply(avail: Option[Seq[String]], msg: Message): Option[SendMessage] = {
    avail.map(a => textMD(msg, s"*Found ${math.min(a.size, 5)}  movies already available in Plex*\n" + a.zipWithIndex.map(s => s"*${s._2 + 1}* - ${s._1}").mkString("\n")))
  }

  def resultReply(results: Option[InlineKeyboardMarkup], msg: Message): Option[SendMessage] = {
    results.map(a => textMD(msg, s"*${math.min(a.inlineKeyboard.head.length, 5)} Search Results Found*").copy(replyMarkup = Some(a)))
  }

  onCommand("searchmovie") { implicit msg =>
    withArgs { args =>
      val q = args.mkString(" ")
      val response = OmbiSearch.searchMovie(q)
      val body: Either[ResponseError[Exception], Seq[OmbiMovieSearchResult]] = response.body
      val lr = body.fold(
        l => noLeft(textMD(msg, "*No Search Results*")),
        r =>
          if (r.isEmpty) {
            noLeft(textMD(msg, "*No Search Results*"))
          }
          else {
            val (avail, results) = resultsMessage(r)
            val lRes = defaultMessage(availableReply(avail, msg), msg, "*No results available in Plex*")
            val rRes = defaultMessage(resultReply(results, msg), msg, "*No Search Results*")

            (Some(lRes), rRes)
          }
      )
      request(lr._2).void.map(_ => lr._1.map(aa => request(aa).void).getOrElse(Future.unit))
    }
  }

  onCommand("coin" or "flip") { implicit msg =>
    reply(if (rng.nextBoolean()) "Head!" else "Tail!").void
  }
  onCommand('real | 'double | 'float) { implicit msg =>
    reply(rng.nextDouble().toString).void
  }
  onCommand("/dice" | "roll") { implicit msg =>
    reply("⚀⚁⚂⚃⚄⚅"(rng.nextInt(6)).toString).void
  }
  onCommand("random" or "rnd") { implicit msg =>
    withArgs {
      case Seq(Int(n)) if n > 0 =>
        reply(rng.nextInt(n).toString).void
      case _ => reply("Invalid argumentヽ(ಠ_ಠ)ノ").void
    }
  }
  onCommand('choose | 'pick | 'select) { implicit msg =>
    withArgs { args =>
      replyMd(if (args.isEmpty) "No arguments provided." else args(rng.nextInt(args.size))).void
    }
  }

  // Int(n) extractor
  object Int {
    def unapply(s: String): Option[Int] = Try(s.toInt).toOption
  }

}

object OmbiBot extends App {
  // To run spawn the bot
  val bot = new OmbiBot()
  val eol = bot.run()
  println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
  scala.io.StdIn.readLine()
  bot.shutdown() // initiate shutdown
  // Wait for the bot end-of-life
  Await.result(eol, Duration.Inf)
}


