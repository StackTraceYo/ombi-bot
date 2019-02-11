package org.stacktrace.yo.plexbot.models.ombi.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class OmbiMovieSearchResponse extends OmbiSearchResponse {


    private String theMovieDbId;
    private String posterPath;

    public String photoPath(){
        return "https://image.tmdb.org/t/p/w300" + posterPath;
    }

    @Override
    public String reqId() {
        return theMovieDbId;
    }
}
