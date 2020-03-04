package org.stacktrace.yo.plexbot.models.tmdb;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Iterator;
import java.util.List;

@Data
@Accessors(chain = true)
public class ResultsPage<T>  implements Iterable<T> {

    @JsonProperty("results")
    private List<T> results;

    @JsonProperty("page")
    private int page;

    @JsonProperty("total_pages")
    private int totalPages;

    @JsonProperty("total_results")
    private int totalResults = 0;

    @Override
    public Iterator<T> iterator() {
        return results.iterator();
    }

}
