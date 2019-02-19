package org.stacktrace.yo.plexbot.models.ombi.response;

import lombok.Data;
import lombok.experimental.Accessors;
import org.stacktrace.yo.plexbot.models.shared.SearchType;

@Data
@Accessors(chain = true)
public abstract class OmbiSearchResponse {

    private String id;
    private Boolean approved;
    private Boolean requested;
    private Integer requestId;
    private Boolean available;
    private String plexUrl;
    private String imdbId;
    private SearchType type;
    private Boolean isDetail;
    private String title;

    public abstract String photoPath();

    public abstract String reqId();
}
