package org.stacktrace.yo.ombi

import com.softwaremill.sttp.MediaTypes
import org.json4s.jackson.Serialization
import sttp.client._
import sttp.client.json4s._
import sttp.model.Header

object OmbiSearch {

  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
  implicit val serialization: Serialization.type = org.json4s.jackson.Serialization


  case class OmbiParams(host: String, key: String, username: Option[String]) {
    def userHeader: String = if (username.isDefined) "UserName" else "ApiAlias"

    def userValue: String = username.getOrElse("ombi-bot")
  }

  def searchMovie(query: String)(implicit ombiParams: OmbiParams): Identity[Response[Either[ResponseError[Exception], Seq[OmbiMovieSearchResult]]]] = {
    //    val request: Request[Either[String, String], Nothing] = basicRequest.get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")

    val search: RequestT[Identity, Either[ResponseError[Exception], Seq[OmbiMovieSearchResult]], Nothing] = basicRequest.get(uri"${ombiParams.host}/api/v1/Search/Movie/$query")
      .headers(
        new Header("ApiKey", ombiParams.key),
        new Header(ombiParams.userHeader, ombiParams.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[Seq[OmbiMovieSearchResult]])

    search.send()

  }

  case class OmbiMovieSearchResult(
                                      theMovieDbId: String,
                                      posterPath: String,
                                      approved: Boolean,
                                      requested: Boolean,
                                      requestId: Integer,
                                      available: Boolean,
                                      plexUrl: String,
                                      imdbId: String,
                                      isDetail: Boolean = false,
                                      title: String,
                                      releaseDate: String
                                    ){
    def isAvailable = available || (plexUrl != null && plexUrl.nonEmpty)

  }

  //  val sort: Option[String] = None
  //  val query = "http language:scala"
  //
  //  // the `query` parameter is automatically url-encoded
  //  // `sort` is removed, as the value is not defined
  //  val request = basicRequest.get(uri"https://api.github.com/search/repositories?q=$query&sort=$sort")
  //
  //  val response = request.send()
  //
  //  // response.header(...): Option[String]
  //  println(response.header("Content-Length"))
  //
  //  // response.body: by default read into an Either[String, String] to indicate failure or success
  //  println(response.body)

}
