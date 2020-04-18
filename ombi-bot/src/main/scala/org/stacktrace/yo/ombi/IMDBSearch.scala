package org.stacktrace.yo.ombi

import java.util.regex.Pattern

import org.jsoup.Jsoup

object IMDBSearch {

  private val IMDB_URL = Pattern.compile("http[s]*://(?:.*\\.|.*)imdb.com/[tT]itle[?/](..\\d+)")

  def looksLike(query: String) = {
    IMDB_URL.matcher(query).find()
  }

  def apply(query: String): String = {
    val matcher = IMDB_URL.matcher(query)
    if (matcher.find) {
      scrapeIMDB(matcher.group(0)).getOrElse(query)
    }
    else query
  }

  def scrapeIMDB(url: String): Option[String] = {
    try {
      val doc = Jsoup.connect(url).get

      val title = doc.title
      // hack
      val i = title.indexOf("(")
      Some(title.substring(0, i).trim)
    }
    catch {
      case e: Throwable => {
        println(e.getMessage)
        Option.empty
      }
    }
  }

}
