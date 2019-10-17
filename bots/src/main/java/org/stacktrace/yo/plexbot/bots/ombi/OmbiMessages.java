package org.stacktrace.yo.plexbot.bots.ombi;

import com.google.common.collect.Lists;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

class OmbiMessages {

    static SendPhoto plexAvailable(Long id, OmbiSearchResponse response) {
        return new SendPhoto()
                .setChatId(id)
                .setPhoto(response.photoPath())
                .setCaption((response.getType() == SearchType.TV ? "TV" : "Movie") + " Is Available")
                .setReplyMarkup(availableKeyboard(response.getPlexUrl()));
    }

    static SendPhoto requestActive(Long id, OmbiSearchResponse response) {
        return new SendPhoto()
                .setChatId(id)
                .setPhoto(response.photoPath())
                .setCaption((response.getType() == SearchType.TV ? "TV" : "Movie") + " has already been requested.");
    }

    static SendMessage nonFound(Long id) {
        return new SendMessage()
                .setChatId(id)
                .setText("No Results");
    }

    static SendPhoto requestSearch(Long id, String requestId, String nextId, OmbiSearchResponse response) {
        return new SendPhoto()
                .setChatId(id)
                .setPhoto(response.photoPath())
                .setCaption((response.getType() == SearchType.TV ? "TV" : "Movie") + " Unavailable")
                .setReplyMarkup(requestKeyboard(requestId, nextId));
    }

    static SendMessage requestSearchNoPhoto(Long id, String requestId, String nextId, OmbiSearchResponse response) {
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

    static InlineKeyboardMarkup requestKeyboard(String requestId, String nextId) {
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
}
