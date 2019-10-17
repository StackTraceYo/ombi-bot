package org.stacktrace.yo.plexbot.bots.ombi;

import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.Commands;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

@Slf4j
final class TVCommand extends OmbiCommand<OmbiTVSearchResponse> {

    TVCommand(OmbiService ombiService, OmbiCallbackHandler handler) {
        super(ombiService, handler, Commands.Ombibot.SEARCH_TV, "Search for a Show To Request");
    }


    @Override
    List<OmbiTVSearchResponse> search(String queryString) {
        return myOmbiService.tvSearch(
                new OmbiSearch()
                        .setSearchType(SearchType.TV)
                        .setQuery(queryString)
        );
    }
}