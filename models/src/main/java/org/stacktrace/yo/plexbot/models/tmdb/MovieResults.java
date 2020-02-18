package org.stacktrace.yo.plexbot.models.tmdb;


import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MovieResults extends ResultsPage<TMDBMovie> {
}
