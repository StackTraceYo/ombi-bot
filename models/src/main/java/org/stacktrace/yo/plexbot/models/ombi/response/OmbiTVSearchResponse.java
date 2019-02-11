package org.stacktrace.yo.plexbot.models.ombi.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class OmbiTVSearchResponse extends OmbiSearchResponse {

    private String title;
    private String banner;
    private String theTvDbId;
    @Override
    public String photoPath() {
        return banner;
    }

    @Override
    public String reqId() {
        return theTvDbId != null ? theTvDbId : getId();
    }
}
