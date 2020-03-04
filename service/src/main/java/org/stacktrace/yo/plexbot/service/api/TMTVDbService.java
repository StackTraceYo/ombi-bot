package org.stacktrace.yo.plexbot.service.api;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.stacktrace.yo.plexbot.models.tmdb.MovieResults;
import org.stacktrace.yo.plexbot.models.tmdb.MovieSearchRequest;
import org.stacktrace.yo.plexbot.models.tmdb.TVSearchRequest;
import org.stacktrace.yo.plexbot.models.tmdb.TVSeriesResults;
import org.stacktrace.yo.plexbot.service.HttpClient;
import org.stacktrace.yo.plexbot.service.api.tmdb.TMDb;

import java.util.Optional;

@Slf4j
public class TMTVDbService implements TMDb {

    static final String TMDB_API_BASE = "https://api.themoviedb.org/3/";
    static final String TVDB_API_URL = "https://api.thetvdb.com/";

    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    private final HttpClient myHttpClient;
    private final TMDBConfig myTMDbConfig;
    private final TVDBConfig myTVDbConfig;
    private final NameValuePair[] mySearchHeaders = new NameValuePair[0];


    public TMTVDbService(HttpClient myhttpClient, TMDBConfig tmdbConfig, TVDBConfig tvdbConfig) {
        myHttpClient = myhttpClient;
        myTMDbConfig = tmdbConfig;
        myTVDbConfig = tvdbConfig;
    }

    public Optional<MovieResults> searchMovie(String query) {
        MovieSearchRequest req = new MovieSearchRequest(query, myTMDbConfig.apiKey);
        return myHttpClient.cacheGet(req.toGetPath(), mySearchHeaders, MovieResults.class);
    }

    public Optional<TVSeriesResults> searchSeries(String query) {
        TVSearchRequest req = new TVSearchRequest(query, myConfig.apiKey);
        return myHttpClient.cacheGet(req.toGetPath(), mySearchHeaders, TVSeriesResults.class);
    }


    public static final class TMDBConfig {
        private final String apiKey;

        public TMDBConfig(String key) {
            this.apiKey = key;
        }
    }

    public static final class TVDBConfig {
        private final String apiKey;

        public TVDBConfig(String key) {
            this.apiKey = key;
        }
    }
}
