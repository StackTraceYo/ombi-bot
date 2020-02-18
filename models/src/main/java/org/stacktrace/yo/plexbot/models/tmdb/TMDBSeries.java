package org.stacktrace.yo.plexbot.models.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TMDBSeries {



    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("original_name")
    private String originalName;
    @JsonProperty("overview")
    private String overview;
    @JsonProperty("backdrop_path")
    private String backdropPath;
    @JsonProperty("poster_path")
    private String posterPath;
    @JsonProperty("first_air_date")
    private String firstAirDate;


}
