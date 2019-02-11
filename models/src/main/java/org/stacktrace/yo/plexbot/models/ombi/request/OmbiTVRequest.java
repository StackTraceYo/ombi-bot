package org.stacktrace.yo.plexbot.models.ombi.request;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import org.stacktrace.yo.plexbot.models.shared.Season;

@Data
@Accessors(chain = true)
public class OmbiTVRequest {

    private Boolean requestAll = false;
    private Boolean latestSeason = false;
    private Boolean firstSeason = false;
    private String tvDbId;
    private List<Season> seasons = new ArrayList<>();

}