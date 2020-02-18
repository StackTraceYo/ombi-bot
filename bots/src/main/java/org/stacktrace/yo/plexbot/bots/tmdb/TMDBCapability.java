package org.stacktrace.yo.plexbot.bots.tmdb;

import com.google.common.collect.Lists;
import org.stacktrace.yo.plexbot.bots.capability.Capability;
import org.stacktrace.yo.plexbot.bots.tmdb.commands.TMDBCallbackHandler;
import org.stacktrace.yo.plexbot.bots.tmdb.commands.TMDBMovieCommand;
import org.stacktrace.yo.plexbot.bots.tmdb.commands.TMDBTVCommand;
import org.stacktrace.yo.plexbot.service.tmdb.TMDBService;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

public class TMDBCapability implements Capability<TMDBCallbackHandler> {

    private final TMDBService tmdbService;

    public TMDBCapability(TMDBService tmdbService) {
        this.tmdbService = tmdbService;
    }

    @Override
    public TMDBCallbackHandler handler(AbsSender sender) {
        return new TMDBCallbackHandler();
    }

    @Override
    public List<IBotCommand> commands(TMDBCallbackHandler handler) {
        return Lists.newArrayList(
                new TMDBMovieCommand(tmdbService),
                new TMDBTVCommand(tmdbService)
        );
    }

    @Override
    public String capabilityName() {
        return "TMDB";
    }

    @Override
    public String info() {
        return "Search for Movies and TV";
    }
}
