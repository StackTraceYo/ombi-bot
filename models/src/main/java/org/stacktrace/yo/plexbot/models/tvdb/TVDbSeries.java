package org.stacktrace.yo.plexbot.models.tvdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TVDbSeries {


    @JsonProperty("id")
    public Integer id;
    @JsonProperty("seriesName")
    public String seriesName;
    @JsonProperty("slug")
    public String slug;
    @JsonProperty("poster")
    public String poster;
    @JsonProperty("banner")
    public String banner;
    @JsonProperty("firstAired")
    public String firstAired;
    @JsonProperty("genre")
    public List<String> genre = new ArrayList<>();
    @JsonProperty("imdbId")
    public String imdbId;

    public String getReleasedYear() {
        if (firstAired != null && !firstAired.isEmpty()) {
            int year = LocalDate.parse(firstAired).getYear();
            return "(" + year + ")";
        } else {
            return "";
        }
    }

}
