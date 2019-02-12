package org.stacktrace.yo.plexbot.bots.ombi;

import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.Commands;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiMovieSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

@Slf4j
public final class MovieCommand extends OmbiCommand {

    MovieCommand(OmbiService ombiService) {
        super(ombiService, Commands.Ombibot.SEARCH_MOVIE, "Search for a Movie To Request");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        List<OmbiMovieSearchResponse> searchResults = myOmbiService.movieSearch(
                new OmbiSearch()
                        .setSearchType(SearchType.MOVIE)
                        .setQuery(String.join(" ", strings))
        );

        handleReply(absSender, user, chat, searchResults);

    }
}