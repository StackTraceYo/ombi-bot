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
public final class TVCommand extends OmbiCommand {

    TVCommand(OmbiService ombiService) {
        super(ombiService, Commands.Ombibot.SEARCH_TV, "Search for a Show To Request");
    }


    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        List<OmbiTVSearchResponse> searchResults = myOmbiService.tvSearch(
                new OmbiSearch()
                        .setSearchType(SearchType.TV)
                        .setQuery(String.join(" ", strings))
        );
        handleReply(absSender, user, chat, searchResults);
    }
}