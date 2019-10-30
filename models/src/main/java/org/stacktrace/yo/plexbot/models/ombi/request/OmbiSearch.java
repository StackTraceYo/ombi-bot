package org.stacktrace.yo.plexbot.models.ombi.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.stacktrace.yo.plexbot.models.shared.Routes;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.models.shared.ToGetRequest;

@Data
@Accessors(chain = true)
@EqualsAndHashCode
public class OmbiSearch implements ToGetRequest {

    private String query;
    private String detail;
    private SearchType searchType;


    @Override
    public String toGetPath() {
        return Routes.Ombi.Search.create(searchType, query);
    }

    @Override
    public String toGetDetailPath() {
        return Routes.Ombi.Search.TV.Detail.create(detail);
    }
}
