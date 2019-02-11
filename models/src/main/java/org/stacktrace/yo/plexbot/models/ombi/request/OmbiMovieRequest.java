package org.stacktrace.yo.plexbot.models.ombi.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OmbiMovieRequest {

    private String theMovieDbId;

}
