package org.stacktrace.yo.plexbot.bots.ombi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiSearchResponse;
import org.stacktrace.yo.plexbot.service.IMDBSearch;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
abstract class OmbiCommand<T extends OmbiSearchResponse> extends BotCommand {

    final OmbiService myOmbiService;
    private final OmbiCallbackHandler myHandler;
    private final Pattern IMDB_URL = Pattern.compile("http[s]*:\\/\\/(?:.*\\.|.*)imdb.com\\/(?:t|T)itle(?:\\?|\\/)(..\\d+)");


    OmbiCommand(OmbiService ombiService, OmbiCallbackHandler handler, String commandIdentifier, String description) {
        super(commandIdentifier, description);
        myHandler = handler;
        myOmbiService = ombiService;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        String query = String.join(" ", strings);
        String imdbTitleOrQuery = checkIMDB(query);
        myHandler.doReply(chat, query, search(imdbTitleOrQuery));
    }

    abstract List<T> search(String queryString);

    private String checkIMDB(String query) {
        return Optional.ofNullable(query)
                .map(search -> {
                    Matcher matcher = IMDB_URL.matcher(search);
                    return matcher.find() ? new IMDBSearch(matcher.group(0)).getTitle() : query;
                }).orElse(query);
    }


    @Data
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static final class OmbiRequestCallback {
        private String action;
        private String sType;
        private String query;
        private String id;
        private Integer index;
    }

}
