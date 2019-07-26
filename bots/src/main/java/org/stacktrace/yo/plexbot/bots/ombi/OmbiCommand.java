package org.stacktrace.yo.plexbot.bots.ombi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.IMDBSearch;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class OmbiCommand extends BotCommand {

    protected final OmbiService myOmbiService;
    private final OmbiBot myBot;
    protected final Pattern IMDB_URL = Pattern.compile("http[s]*:\\/\\/(?:.*\\.|.*)imdb.com\\/(?:t|T)itle(?:\\?|\\/)(..\\d+)");


    public OmbiCommand(OmbiBot bot, OmbiService ombiService, String commandIdentifier, String description) {
        super(commandIdentifier, description);
        myBot = bot;
        myOmbiService = ombiService;
    }

    protected String checkIMDB(String query) {
        return Optional.ofNullable(query)
                .map(search -> {
                    Matcher matcher = IMDB_URL.matcher(search);
                    boolean found = matcher.find();
                    if (found) {
                        return new IMDBSearch(matcher.group(0)).getTitle();
                    } else {
                        return query;
                    }
                }).orElse(query);
    }


    SendPhoto plexAvailable(Long id, OmbiSearchResponse response) {
        return new SendPhoto()
                .setChatId(id)
                .setPhoto(response.photoPath())
                .setCaption((response.getType() == SearchType.TV ? "TV" : "Movie") + " Is Available")
                .setReplyMarkup(availableKeyboard(response.getPlexUrl()));
    }

    SendPhoto requestActive(Long id, OmbiSearchResponse response) {
        return new SendPhoto()
                .setChatId(id)
                .setPhoto(response.photoPath())
                .setCaption((response.getType() == SearchType.TV ? "TV" : "Movie") + " has already been requested.");
    }

    SendMessage nonFound(Long id) {
        return new SendMessage()
                .setChatId(id)
                .setText("No Results");
    }

    static SendPhoto requestSearch(Long id, String requestId, String nextId, OmbiSearchResponse response) throws IOException {
        return new SendPhoto()
                .setChatId(id)
                .setPhoto(response.photoPath())
                .setCaption((response.getType() == SearchType.TV ? "TV" : "Movie") + " Unavailable")
                .setReplyMarkup(requestKeyboard(requestId, nextId));
    }

    static SendMessage requestSearchNoPhoto(Long id, String requestId, String nextId, OmbiSearchResponse response) throws IOException {
        return new SendMessage()
                .setChatId(id)
                .setText("No Image found for " + response.getTitle() + "\n" + (response.getType() == SearchType.TV ? "TV" : "Movie") + " Unavailable")
                .setReplyMarkup(requestKeyboard(requestId, nextId));
    }

    static InlineKeyboardMarkup availableKeyboard(String url) {
        List<InlineKeyboardButton> keyboardButtons = Lists.newArrayList(
                new InlineKeyboardButton()
                        .setText("View In Plex")
                        .setUrl(url)
        );
        List<List<InlineKeyboardButton>> rows = Lists.newArrayList();
        rows.add(keyboardButtons);
        return new InlineKeyboardMarkup()
                .setKeyboard(rows);
    }

    static InlineKeyboardMarkup requestKeyboard(String requestId, String nextId) throws JsonProcessingException {
        List<InlineKeyboardButton> keyboardButtons = null;

        keyboardButtons = Lists.newArrayList(
                new InlineKeyboardButton()
                        .setText("Request")
                        .setCallbackData(requestId),
                new InlineKeyboardButton()
                        .setText("Next Result")
                        .setCallbackData(nextId)
        );
        List<List<InlineKeyboardButton>> rows = Lists.newArrayList();
        rows.add(keyboardButtons);
        return new InlineKeyboardMarkup().setKeyboard(rows);
    }

    protected <T extends OmbiSearchResponse> void initialReply(AbsSender sender, User user, Chat chat, String query, List<T> searchResults) {
        try {
            if (!searchResults.isEmpty()) {
                T result = searchResults.get(0);
                if (result.getAvailable()) {
                    log.debug("Replying with Available in Plex");
                    sender.execute(plexAvailable(chat.getId(), result));

                } else if (result.getApproved() || result.getRequested()) {
                    log.debug("Replying with Request Active");
                    sender.execute(requestActive(chat.getId(), result));
                } else {
                    log.debug("Replying with Requesting Search");
                    OmbiRequestCallback req1 = new OmbiRequestCallback().setAction("req")
                            .setSType(result.getType().getReqValue())
                            .setId(result.getId());

                    OmbiRequestCallback next = new OmbiRequestCallback().setAction("next")
                            .setSType(result.getType().getReqValue())
                            .setIndex(0)
                            .setQuery(query);

                    String reqId = UUID.randomUUID().toString();
                    String nextId = UUID.randomUUID().toString();
                    myBot.addCallback(reqId, req1);
                    myBot.addCallback(nextId, next);
                    if (result.photoPath() == null) {
                        sender.execute(requestSearchNoPhoto(chat.getId(), reqId, nextId, result));
                    } else {
                        sender.execute(requestSearch(chat.getId(), reqId, nextId, result));
                    }

                }
            } else {
                log.debug("Replying with Not Found");
                sender.execute(nonFound(chat.getId()));
            }
        } catch (Exception e) {
            log.error("Error Sending Message", e);
        }
    }

    @Data
    @Accessors(chain = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class OmbiRequestCallback {
        private String action;
        private String sType;
        private String query;
        private String id;
        private Integer index;
    }

    public static void main(String[] args) {
        final Pattern IMDB_URL = Pattern.compile("http[s]*:\\/\\/(?:.*\\.|.*)imdb.com\\/(?:t|T)itle(?:\\?|\\/)(..\\d+)");
        Matcher matcher = IMDB_URL.matcher("https://m.imdb.com/title/tt4669296/");
        matcher.find();
        String group = matcher.group(0);
        IMDBSearch imdbSearch = new IMDBSearch(group);
        imdbSearch.getTitle();
    }

}
