package org.stacktrace.yo.plexbot.bots.ombi;

import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.Commands;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;

import java.util.List;

@Slf4j
final class TVCommand extends OmbiCommand<OmbiTVSearchResponse> {

    TVCommand(OmbiService ombiService, OmbiCallbackHandler handler) {
        super(ombiService, handler, Commands.Ombibot.SEARCH_TV, "Search for a Show To Request");
    }


    @Override
    List<OmbiTVSearchResponse> search(String queryString) {
        List<OmbiTVSearchResponse> ombiTVSearchResponses = myOmbiService.tvSearch(
                new OmbiSearch()
                        .setSearchType(SearchType.TV)
                        .setQuery(queryString)
        );
        if (!ombiTVSearchResponses.isEmpty()) {
            OmbiTVSearchResponse firstResult = ombiTVSearchResponses.get(0);
            // for whatever reason ombi doesnt tell me what i need to know with the basic search.
            myOmbiService.tvDetail(new OmbiSearch()
                    .setSearchType(SearchType.TV)
                    .setQuery(queryString)
                    .setDetail(firstResult.getId()))
                    .ifPresent(firstResult::updateAvailabilityFrom);
        }
        return ombiTVSearchResponses;
    }
}