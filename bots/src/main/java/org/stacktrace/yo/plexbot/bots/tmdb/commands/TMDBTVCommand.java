package org.stacktrace.yo.plexbot.bots.tmdb.commands;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.Commands;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVDetailResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.models.tmdb.TMDBSeries;
import org.stacktrace.yo.plexbot.models.tmdb.TVSeriesResults;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.stacktrace.yo.plexbot.service.api.TMTVDbService;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public final class TMDBTVCommand extends BotCommand {

    private final TMTVDbService myTMDBService;
    private final OmbiService myOmbiservice;

    public TMDBTVCommand(TMTVDbService tvService, OmbiService ombiService) {
        super(Commands.TMDBBot.SEARCH_TV, "Search for a Show In TMdb");
        myTMDBService = tvService;
        myOmbiservice = ombiService;
    }

    Optional<TVSeriesResults> search(String queryString) {
        return myTMDBService.searchSeries(queryString)
                .map(r -> r.getTotalResults() <= 0 ? null : r);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        try {
            String query = String.join(" ", strings);
            SendMessage message = search(query)
                    .map((res) -> createTVResultsMessage(chat, res))
                    .orElse(noneFoundMessage(chat));
            absSender.execute(message);
        } catch (Exception e) {
            log.error("Unable to Execute command", e);
        }
    }

    private List<InlineKeyboardButton> requestRow(TMDBSeries tmdbSeries) {
        List<InlineKeyboardButton> row = Lists.newArrayList();
        row.add(new InlineKeyboardButton()
                .setText(createResult(tmdbSeries))
                .setCallbackData("1")
        );
        row.add(new InlineKeyboardButton()
                .setText("TMDB")
                .setUrl("https://www.themoviedb.org/tv/" + tmdbSeries.getId()));
        return row;
    }

    private List<InlineKeyboardButton> getRow(List<TMDBSeries> seriesList, int idx) {
        TMDBSeries tmdbSeries = seriesList.get(idx);
        Optional<OmbiTVDetailResponse> ombiTVDetailResponse = myOmbiservice.tvDetail(new OmbiSearch().setDetail(tmdbSeries.getId()).setSearchType(SearchType.TV));
        return ombiTVDetailResponse.map(res -> {
            if (res.getAvailable()) {
                return availableKeyboard(res.getPlexUrl());
            } else {
                return requestRow(tmdbSeries);
            }
        }).orElse(requestRow(tmdbSeries));
    }

    private SendMessage createTVResultsMessage(Chat chat, TVSeriesResults res) {
        int take = Math.min(res.getTotalResults(), 5);
        List<TMDBSeries> seriesList = res.getResults();

        List<List<InlineKeyboardButton>> rows = IntStream.range(0, take)
                .mapToObj(i -> getRow(seriesList, i))
                .collect(Collectors.toList());

        SendMessage message = new SendMessage()
                .setChatId(chat.getId())
                .enableMarkdown(true)
                .setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(rows))
                .setText("*" + res.getTotalResults() + "* Results Found\n Click to Request\n");

        if (res.getTotalPages() > 1) {
            message.setReplyMarkup(nextPage(res.getPage() + 1));
        }
        return message;
    }

    private String createResult(TMDBSeries series) {
        return series.getName() + " " + series.getReleasedYear();
    }

    private SendMessage noneFoundMessage(Chat chat) {
        return new SendMessage()
                .setChatId(chat.getId())
                .setText("No Results Found :(");
    }

    static InlineKeyboardMarkup nextPage(int page) {
        List<InlineKeyboardButton> keyboardButtons = null;

        keyboardButtons = Lists.newArrayList(
                new InlineKeyboardButton()
                        .setText("Next Page")
                        .setCallbackData(String.valueOf(page))
        );
        List<List<InlineKeyboardButton>> rows = Lists.newArrayList();
        rows.add(keyboardButtons);
        return new InlineKeyboardMarkup().setKeyboard(rows);
    }

    static List<InlineKeyboardButton> availableKeyboard(String url) {
        return Lists.newArrayList(
                new InlineKeyboardButton()
                        .setText("View In Plex")
                        .setUrl(url)
        );

    }
}