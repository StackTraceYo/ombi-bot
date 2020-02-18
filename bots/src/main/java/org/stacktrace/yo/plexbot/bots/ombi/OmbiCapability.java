package org.stacktrace.yo.plexbot.bots.ombi;

import com.google.common.collect.Lists;
import org.stacktrace.yo.plexbot.bots.capability.Capability;
import org.stacktrace.yo.plexbot.bots.ombi.commands.MovieCommand;
import org.stacktrace.yo.plexbot.bots.ombi.commands.OmbiCallbackHandler;
import org.stacktrace.yo.plexbot.bots.ombi.commands.TVCommand;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

public class OmbiCapability implements Capability<OmbiCallbackHandler> {

    private final String info = "Commands Available:\n\n" +
            "To search a movie use the /searchmovie command followed by a <query>\n\n" +
            "\tExample: /searchmovie the dark knight\n\n" +
            "To search a TV show use the /searchtv command followed by a <query>\n\n" +
            "\tExample: /searchtv star wars rebels\n\n" +
            "\t\tNote: Both /searchtv and /searchmovie commands work with imdb urls\n\n" +
            "\t\tExample /searchtv https://www.imdb.com/title/tt2568204\n\n" +
            "/info to see this message\n";


    private final OmbiService myOmbi;

    public OmbiCapability(OmbiService ombiService) {
        this.myOmbi = ombiService;
    }

    @Override
    public OmbiCallbackHandler handler(AbsSender sender) {
        return new OmbiCallbackHandler(myOmbi, sender);
    }

    @Override
    public List<IBotCommand> commands(OmbiCallbackHandler handler) {
        return Lists.newArrayList(
                new MovieCommand(myOmbi, handler),
                new TVCommand(myOmbi, handler)
        );
    }

    @Override
    public String capabilityName() {
        return "OMBI";
    }

    @Override
    public String info() {
        return info;
    }

}
