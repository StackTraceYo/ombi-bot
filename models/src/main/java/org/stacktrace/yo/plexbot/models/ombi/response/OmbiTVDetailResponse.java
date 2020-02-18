package org.stacktrace.yo.plexbot.models.ombi.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.stacktrace.yo.plexbot.models.ombi.Season;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class OmbiTVDetailResponse extends OmbiTVSearchResponse {


    private List<Season> seasonRequests;

    @Override
    public String photoPath() {
        return getBanner();
    }

    @Override
    public String reqId() {
        return getTheTvDbId() != null ? getTheTvDbId(): getId();
    }
}
