package org.stacktrace.yo.plexbot.models.shared;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public final class Episodes {
    private Integer episodeNumber;
}