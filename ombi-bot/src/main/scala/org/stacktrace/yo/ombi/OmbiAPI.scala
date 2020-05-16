package org.stacktrace.yo.ombi

import com.softwaremill.sttp.MediaTypes
import org.json4s.jackson.Serialization
import sttp.client._
import sttp.client.json4s._
import sttp.model.Header

object OmbiAPI {


  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
  implicit val serialization: Serialization.type = org.json4s.jackson.Serialization

  def searchMovie(query: String)(implicit ombi: OmbiParams): Identity[Response[Either[ResponseError[Exception], Seq[OmbiMovieSearchResult]]]] = {
    basicRequest.get(uri"${ombi.host}/api/v1/Search/Movie/$query")
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[Seq[OmbiMovieSearchResult]])
      .mapResponse(e => e.fold(l => Left(l), r => Right(r.map(movieDetail))))
      .send()
  }

  def searchTV(query: String)(implicit ombi: OmbiParams): Identity[Response[Either[ResponseError[Exception], Seq[OmbiTVSearchResult]]]] = {
    basicRequest.get(uri"${ombi.host}/api/v1/Search/Tv/$query")
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[Seq[OmbiTVSearchResult]])
      .mapResponse(e => e.fold(l => Left(l), r => Right(r.map(tvDetail))))
      .send()
  }

  def tvDetail(result: OmbiTVSearchResult)(implicit ombi: OmbiParams): OmbiTVSearchResult = {
    basicRequest.get(uri"${ombi.host}/api/v1/Search/Tv/info/${result.infoId}")
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[OmbiTVSearchResult])
      .mapResponse(e => e.fold(_ => result, r => result.merge(r)))
      .send()
      .body
  }

  def movieDetail(result: OmbiMovieSearchResult)(implicit ombi: OmbiParams): OmbiMovieSearchResult = {
    basicRequest.get(uri"${ombi.host}/api/v1/Search/Movie/info/${result.infoId}")
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[OmbiMovieSearchResult])
      .mapResponse(e => e.fold(_ => result, r => result.merge(r)))
      .send()
      .body
  }

  def requestMovie(id: String)(implicit ombi: OmbiParams): Identity[Response[Either[String, String]]] = {
    basicRequest.post(uri"${ombi.host}/api/v1/Request/Movie/").body(OmbiMovieRequest(id))
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .send()
  }

  def requestTV(id: String)(implicit ombi: OmbiParams): Identity[Response[Either[String, String]]] = {
    basicRequest.get(uri"${ombi.host}/api/v1/Search/Tv/info/${id}")
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[OmbiTVSearchResult])
      .mapResponse(e => e.fold(
        l => Left(l),
        r => Right(basicRequest.post(uri"${ombi.host}/api/v1/Request/Tv/").body(OmbiTVRequest(id, seasons = r.seasonRequests))
          .headers(
            new Header("ApiKey", ombi.key),
            new Header(ombi.userHeader, ombi.userValue),
            new Header("Content-Type", MediaTypes.Json)
          )
          .send()
          .body
        )
      ).fold(l => Left(l.body), r => Right(r.getOrElse("Error"))))
      .send()
  }


  case class OmbiParams(host: String, key: String, username: Option[String]) {
    def userHeader: String = if (username.isDefined) "UserName" else "ApiAlias"

    def userValue: String = username.getOrElse("ombi-bot")
  }

  case class Season(seasonNumber: Integer, episodes: Seq[Episodes] = Seq())

  case class Episodes(episodeNumber: Integer)

  case class OmbiMovieRequest(theMovieDbId: String)

  case class OmbiTVRequest(tvDbId: String, requestAll: Boolean = true, latestSeason: Boolean = false, firstSeason: Boolean = false, seasons: Seq[Season] = Seq())

  case class OmbiMovieSearchResult(theMovieDbId: String, posterPath: String, approved: Boolean, requested: Boolean, requestId: Integer, available: Boolean, plexUrl: String, isDetail: Boolean = false, title: String, releaseDate: String, id: String, imdbId: String) {
    def isAvailable: Boolean = available || (plexUrl != null && plexUrl.nonEmpty)

    def infoId: String = if (theMovieDbId != null) {
      theMovieDbId
    } else {
      id
    }

    def merge(r: OmbiMovieSearchResult): OmbiMovieSearchResult = {
      copy(approved = r.approved, available = r.available, requested = r.requested, plexUrl = r.plexUrl, imdbId = r.imdbId)
    }
  }

  case class OmbiTVSearchResult(theTvDbId: String, banner: String, approved: Boolean, requested: Boolean, requestId: Integer, available: Boolean, plexUrl: String, isDetail: Boolean = false, title: String, firstAired: String, id: String, imdbId: String, seasonRequests: Seq[Season] = Seq()) {
    def isAvailable: Boolean = available || (plexUrl != null && plexUrl.nonEmpty)

    def merge(r: OmbiTVSearchResult): OmbiTVSearchResult = {
      copy(approved = r.approved, available = r.available, requested = r.requested, plexUrl = r.plexUrl, theTvDbId = r.theTvDbId, imdbId = r.imdbId)
    }

    def infoId: String = if (theTvDbId != null) {
      theTvDbId
    } else {
      id
    }

  }

}
