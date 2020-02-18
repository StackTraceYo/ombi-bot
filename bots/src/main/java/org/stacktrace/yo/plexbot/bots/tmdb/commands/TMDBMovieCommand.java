package org.stacktrace.yo.plexbot.bots.tmdb.commands;

import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.Commands;
import org.stacktrace.yo.plexbot.models.tmdb.MovieResults;
import org.stacktrace.yo.plexbot.service.tmdb.TMDBService;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public final class TMDBMovieCommand extends BotCommand {

    private final TMDBService myTMDBService;

    public TMDBMovieCommand(TMDBService tvService ) {
        super(Commands.TMDBBot.SEARCH_MOVIE, "Search for a Movie In Imdb");
        myTMDBService =  tvService;
    }

    MovieResults search(String queryString) {
        return myTMDBService.searchMovie(queryString).get();
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        try {
            String query = String.join(" ", strings);
            absSender.execute(createInfoMessage(chat, search(query)));
        } catch (Exception e) {
            log.error("Unable to Execute command", e);
        }
    }

    private SendMessage createInfoMessage(Chat chat, MovieResults res) {
        return new SendMessage()
                .setChatId(chat.getId())
                .setText(res.getResults().get(0).toString());
    }
}