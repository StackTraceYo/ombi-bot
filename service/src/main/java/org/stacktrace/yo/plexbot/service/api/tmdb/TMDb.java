package org.stacktrace.yo.plexbot.service.api.tmdb;

import org.stacktrace.yo.plexbot.models.tmdb.MovieResults;
import org.stacktrace.yo.plexbot.models.tmdb.TVSeriesResults;

import java.util.Optional;

public interface TMDb {



    Optional<MovieResults> searchMovie(String query);

    Optional<TVSeriesResults> searchSeries(String query);
}
