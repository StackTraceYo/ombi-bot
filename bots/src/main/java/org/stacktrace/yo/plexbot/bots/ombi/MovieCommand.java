package org.stacktrace.yo.plexbot.bots.ombi;

import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.Commands;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiMovieSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;

import java.util.List;

@Slf4j
final class MovieCommand extends OmbiCommand<OmbiMovieSearchResponse> {

    MovieCommand(OmbiService ombiService, OmbiCallbackHandler callbackHandler) {
        super(ombiService, callbackHandler, Commands.Ombibot.SEARCH_MOVIE, "Search for a Movie To Request");
    }

    @Override
    List<OmbiMovieSearchResponse> search(String queryString) {
        return myOmbiService.movieSearch(
                new OmbiSearch()
                        .setSearchType(SearchType.MOVIE)
                        .setQuery(queryString)
        );
    }
}