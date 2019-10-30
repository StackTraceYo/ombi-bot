package org.stacktrace.yo.plexbot.bots.ombi;

import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.Commands;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Slf4j
public final class InfoCommand extends BotCommand {

    private final String info = "Commands Available:\n\n" +
            "To search a movie use the /searchmovie command followed by a <query>\n\n" +
            "\tExample: /searchmovie the dark knight\n\n" +
            "To search a TV show use the /searchtv command followed by a <query>\n\n" +
            "\tExample: /searchtv star wars rebels\n\n" +
            "\t\tNote: Both /searchtv and /searchmovie commands work with imdb urls\n\n" +
            "\t\ttExample /searchtv https://www.imdb.com/title/tt2568204\n\n" +
            "/info to see this message\n";

    InfoCommand() {
        super(Commands.Ombibot.INFO, "Information About this Bot");
    }


    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        try {
            absSender.execute(createInfoMessage(chat));
        } catch (Exception e) {
            log.error("Unable to Execute command", e);
        }
    }

    private SendMessage createInfoMessage(Chat chat) {
        return new SendMessage()
                .setChatId(chat.getId())
                .setText(info);
    }
}