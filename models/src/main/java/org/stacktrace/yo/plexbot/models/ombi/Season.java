package org.stacktrace.yo.plexbot.models.ombi;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public final class Season {
    private Integer seasonNumber;
    private List<Episodes> episodes = new ArrayList<>();
}