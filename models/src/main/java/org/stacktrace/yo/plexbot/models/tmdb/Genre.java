package org.stacktrace.yo.plexbot.models.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Genre {

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
}
