package org.stacktrace.yo.ombi

import com.softwaremill.sttp.MediaTypes
import org.json4s.jackson.Serialization
import slogging.LazyLogging
import sttp.client._
import sttp.client.json4s._
import sttp.model.Header

object OmbiAPI extends LazyLogging {


  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
  implicit val serialization: Serialization.type = org.json4s.jackson.Serialization


  def searchAll(query: String)(implicit ombi: OmbiParams): Identity[Response[Either[ResponseError[Exception], Seq[(Option[OmbiMovieSearchResult], Option[OmbiTVSearchResult])]]]] = {
    basicRequest.post(uri"${ombi.host}/api/v2/search/multi/$query")
      .body(MultiSearch(movies = true, tvShows = true))
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[Seq[MultiSearchRes]])
      .mapResponse(e => e.fold(
        l => {
          logger.error(s"Error: ${l.getMessage} BODY: ${l.body}")
          Left(l)
        },
        r => {
          val all = r.map(rr => {
            rr.mediaType match {
              case "movie" => (Some(movieDetail(rr)), Option.empty)
              case "tv" => (Option.empty, Some(tvDetail(rr)))
              case _ => (Option.empty, Option.empty)
            }
          })
          logger.info(s"Search Results: ${all.mkString(" ")}")
          Right(all)
        }))
      .send()
  }

  def searchMovie(query: String)(implicit ombi: OmbiParams): Identity[Response[Either[ResponseError[Exception], Seq[OmbiMovieSearchResult]]]] = {
    basicRequest.post(uri"${ombi.host}/api/v2/search/multi/$query")
      .body(MultiSearch(movies = true))
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[Seq[MultiSearchRes]])
      .mapResponse(e => e.fold(
        l => {
          logger.error(s"Error: ${l.getMessage} BODY: ${l.body}")
          Left(l)
        },
        r => {
          val movies = r.filter(_.mediaType == "movie")
          logger.info(s"Search Results: ${movies.map(_.toString).mkString(" ")}")
          Right(movies.map(movieDetail))
        }))
      .send()
  }

  def searchTV(query: String)(implicit ombi: OmbiParams): Identity[Response[Either[ResponseError[Exception], Seq[OmbiTVSearchResult]]]] = {
    basicRequest.post(uri"${ombi.host}/api/v2/search/multi/$query")
      .body(MultiSearch(tvShows = true))
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[Seq[MultiSearchRes]])
      .mapResponse(e => e.fold(
        l => {
          logger.error(s"Error: ${l.getMessage} BODY: ${l.body}")
          Left(l)
        },
        r => {
          val tvs = r.filter(_.mediaType == "tv")
          logger.info(tvs.map(_.toString).mkString(" "))
          Right(tvs.map(tvDetail))
        }))
      .send()
  }


  def tvDetail(result: MultiSearchRes)(implicit ombi: OmbiParams): OmbiTVSearchResult = {
    basicRequest.get(uri"${ombi.host}/api/v2/Search/Tv/moviedb/${result.id}")
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[OmbiTVSearchResult])
      .mapResponse(e => e.fold(
        l => {
          logger.error(s"Error: ${l.getMessage} BODY: ${l.body}")
          null
        },
        r => {
          logger.info(s"${r}")
          r
        }
      ))
      .send()
      .body
  }

  def movieDetail(result: MultiSearchRes)(implicit ombi: OmbiParams): OmbiMovieSearchResult = {
    basicRequest.get(uri"${ombi.host}/api/v1/Search/Movie/info/${result.id}")
      .headers(
        new Header("ApiKey", ombi.key),
        new Header(ombi.userHeader, ombi.userValue),
        new Header("Content-Type", MediaTypes.Json)
      )
      .response(asJson[OmbiMovieSearchResult])
      .mapResponse(e => e.fold(
        l => {
          logger.error(s"Error: ${l.getMessage} BODY: ${l.body}")
          null
        },
        r => {
          logger.info(s"${r}")
          r
        }
      ))
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
        l => {
          logger.error(s"Error: ${l.getMessage} BODY: ${l.body}")
          Left(l)
        },
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

  }

  case class OmbiTVSearchResult(theTvDbId: String, banner: String, approved: Boolean, requested: Boolean, requestId: Integer, available: Boolean, plexUrl: String, isDetail: Boolean = false, title: String, firstAired: String, id: String, imdbId: String, seasonRequests: Seq[Season] = Seq()) {
    def isAvailable: Boolean = available || (plexUrl != null && plexUrl.nonEmpty)

    def infoId: String = if (theTvDbId != null) {
      theTvDbId
    } else {
      id
    }
  }

  case class MultiSearch(movies: Boolean = false, music: Boolean = false, people: Boolean = false, tvShows: Boolean = false)

  case class MultiSearchRes(id: String, mediaType: String, poster: String, title: String)

}
