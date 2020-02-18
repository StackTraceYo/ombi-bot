package org.stacktrace.yo.plexbot.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.stacktrace.yo.plexbot.models.tmdb.MovieResults;
import org.stacktrace.yo.plexbot.models.tmdb.MovieSearchRequest;
import org.stacktrace.yo.plexbot.models.tmdb.TVSearchRequest;
import org.stacktrace.yo.plexbot.models.tmdb.TVSeriesResults;
import org.stacktrace.yo.plexbot.service.HttpClient;

import java.util.Optional;

@Slf4j
public class TMDBService {

    private static final String TMDB_API_BASE = "https://api.themoviedb.org/3/";


    private final HttpClient myHttpClient;
    private final TMDBConfig myConfig;
    private final NameValuePair[] mySearchHeaders = new NameValuePair[0];


    public TMDBService(HttpClient myhttpClient, TMDBConfig config) {
        myHttpClient = myhttpClient;
        myConfig = config;
    }

    public Optional<MovieResults> searchMovie(String query) {
        MovieSearchRequest req = new MovieSearchRequest(query, myConfig.apiKey);
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
}
