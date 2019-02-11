package org.stacktrace.yo.plexbot.bots.ombi;

import java.util.List;
import org.stacktrace.yo.plexbot.bots.Commands;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public final class TVCommand extends OmbiCommand {

    TVCommand(OmbiService ombiService) {
        super(ombiService, Commands.Ombibot.SEARCH_TV, "Search for a Show To Request");
    }


    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        List<OmbiTVSearchResponse> tv = myOmbiService.tvSearch(
                new OmbiSearch()
                        .setSearchType(SearchType.TV)
                        .setQuery(String.join(" ", strings))
        );


        if (!tv.isEmpty()) {
            if (tv.get(0).getAvailable()) {
                try {
                    absSender.execute(plexAvailable(chat.getId(), tv.get(0)));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    absSender.execute(requestSearch(chat.getId(), tv.get(0)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {

            try {
                absSender.execute(nonFound(chat.getId()));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

        }

    }
}