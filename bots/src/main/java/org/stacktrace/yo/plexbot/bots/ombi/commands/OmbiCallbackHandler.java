package org.stacktrace.yo.plexbot.bots.ombi.commands;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.stacktrace.yo.plexbot.bots.capability.CallbackHandler;
import org.stacktrace.yo.plexbot.bots.ombi.OmbiMessages;
import org.stacktrace.yo.plexbot.bots.ombi.commands.OmbiCommand;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiTVRequest;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiMovieSearchResponse;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiSearchResponse;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVDetailResponse;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class OmbiCallbackHandler implements CallbackHandler {

    private final Map<String, OmbiCommand.OmbiRequestCallback> myCallbacks;
    private final AbsSender mySender;
    private final OmbiService myOmbiService;

    public OmbiCallbackHandler(OmbiService myOmbiService, AbsSender mySender) {
        this.myOmbiService = myOmbiService;
        this.mySender = mySender;
        this.myCallbacks = Maps.newHashMap();
    }

    public void doCallBack(Update update) {
        CallbackQuery query = update.getCallbackQuery();
        if (StringUtils.isNotBlank(query.getData())) {
            String cbId = query.getData();
            OmbiCommand.OmbiRequestCallback cb = myCallbacks.remove(cbId);
            String action = cb == null ? "na" : cb.getAction();
            switch (action) {
                case "req":
                    doRequest(update, cb);
                    return;
                case "next":
                    doNext(update, cb);
                    return;
                default:
                    sendMissing(update.getMessage().getChatId());
            }
        } else {
            log.info("Callback had no Data");
        }
    }

    <T extends OmbiSearchResponse> void doReply(Chat chat, String query, List<T> searchResults) {

        if (!searchResults.isEmpty()) {
            T result = searchResults.get(0);
            if (result.getAvailable()) {
                log.debug("Replying with Available in Plex");
                sendPhoto(OmbiMessages.plexAvailable(chat.getId(), result));
            } else if (result.getApproved() || result.getRequested()) {
                log.debug("Replying with Request Active");
                sendPhoto(OmbiMessages.requestActive(chat.getId(), result));
            } else {
                log.debug("Replying with Requesting Search");
                String[] ids = createCallBacks(result, 0, query);

                if (result.photoPath() != null) {
                    sendPhoto(OmbiMessages.requestSearch(chat.getId(), ids[0], ids[1], result));
                } else {
                    sendMessage(OmbiMessages.requestSearchNoPhoto(chat.getId(), ids[0], ids[1], result));
                }
            }
        } else {
            log.debug("Replying with Not Found");
            sendMessage(OmbiMessages.nonFound(chat.getId()));
        }
    }

    public void clear() {
        myCallbacks.clear();
    }

    private void removeMessage(Update update) {
        removeMessage(update.getCallbackQuery().getMessage().getMessageId(), update.getCallbackQuery().getMessage().getChatId());
    }

    private void sendSuccessAndRemove(Update update) {
        sendSuccessRequest(update.getCallbackQuery().getMessage().getChatId());
        removeMessage(update);
    }

    private void doRequest(Update update, OmbiCommand.OmbiRequestCallback cb) {
        switch (SearchType.fromReqValue(cb.getSType())) {
            case MOVIE:
                handleMovieRequest(update, cb);
                return;
            case TV:
                handleTVRequest(update, cb);
                return;
            case NA:
                sendError(update.getMessage().getChatId(), "Unknown Request Type");
        }
    }

    private void doNext(Update update, OmbiCommand.OmbiRequestCallback cb) {
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        try {
            switch (SearchType.fromReqValue(cb.getSType())) {
                case MOVIE:
                    handleNextMovie(cb, chatId, messageId);
                case TV:
                    handleNextTv(cb, chatId);
                    break;
            }
        } catch (Exception e) {
            sendError(chatId, "Error with getting next request");
            e.printStackTrace();
        }
    }

    private void handleMovieRequest(Update update, OmbiCommand.OmbiRequestCallback cb) {
        Optional<Map> requested = myOmbiService.requestMovieByTMDB(cb.getId());
        if (requested.isPresent()) {
            sendSuccessAndRemove(update);
        } else {
            sendError(update.getMessage().getChatId(), "Error Sending Request for Movie Id " + cb.getId());
            removeMessage(update);
        }
    }

    private void handleTVRequest(Update update, OmbiCommand.OmbiRequestCallback cb) {
        Optional<OmbiTVDetailResponse> detail = myOmbiService.tvDetail(new OmbiSearch().setSearchType(SearchType.TV).setDetail(cb.getId()));
        if (detail.isPresent()) {
            OmbiTVDetailResponse ombiTVDetailResponse = detail.get();
            Optional<Map> requested = myOmbiService.request(
                    new OmbiTVRequest()
                            .setTvDbId(cb.getId())
                            .setRequestAll(true)
                            .setSeasons(ombiTVDetailResponse.getSeasonRequests())
            );
            if (requested.isPresent()) {
                sendSuccessAndRemove(update);
            } else {
                sendError(update.getMessage().getChatId(), "Error Sending Request");
                removeMessage(update);
            }
        } else {
            sendError(update.getMessage().getChatId(), "Error Finding Movie Detail for TV Id" + cb.getId());
            removeMessage(update);
        }
    }

    private void handleNextMovie(OmbiCommand.OmbiRequestCallback cb, Long chatId, Integer messageId) {
        int nextIdx = cb.getIndex() + 1;

        List<OmbiMovieSearchResponse> searchResultsMovie = myOmbiService.movieSearch(
                new OmbiSearch()
                        .setSearchType(SearchType.MOVIE)
                        .setQuery(cb.getQuery())
        );

        if (nextIdx >= searchResultsMovie.size()) {
            sendNoMoreResults(chatId);
        } else {
            OmbiMovieSearchResponse result = searchResultsMovie.get(nextIdx);
            String[] ids = createCallBacks(result, nextIdx, cb.getQuery());
            if (result.getPosterPath() != null) {
                sendPhoto(OmbiMessages.requestSearch(chatId, ids[0], ids[1], result));
            } else {
                sendMessage(OmbiMessages.requestSearchNoPhoto(chatId, ids[0], ids[1], result));
            }
            removeMessage(messageId, chatId);
        }
    }

    private void handleNextTv(OmbiCommand.OmbiRequestCallback cb, Long chatId) {
        int nextIdx = cb.getIndex() + 1;

        List<OmbiTVSearchResponse> searchResultsTv = myOmbiService.tvSearch(
                new OmbiSearch()
                        .setSearchType(SearchType.TV)
                        .setQuery(cb.getQuery())
        );
        if (nextIdx >= searchResultsTv.size()) {
            sendNoMoreResults(chatId);
        } else {
            OmbiTVSearchResponse result = searchResultsTv.get(nextIdx);
            String[] ids = createCallBacks(result, nextIdx, cb.getQuery());
            if (result.photoPath() != null) {
                sendPhoto(OmbiMessages.requestSearch(chatId, ids[0], ids[1], result));
            } else {
                sendMessage(OmbiMessages.requestSearchNoPhoto(chatId, ids[0], ids[1], result));
            }
        }
    }

    private String[] createCallBacks(OmbiSearchResponse result, Integer nextIdx, String query) {
        OmbiCommand.OmbiRequestCallback req1 = new OmbiCommand.OmbiRequestCallback().setAction("req")
                .setSType(result.getType().getReqValue())
                .setId(result.getId());

        OmbiCommand.OmbiRequestCallback next = new OmbiCommand.OmbiRequestCallback().setAction("next")
                .setSType(result.getType().getReqValue())
                .setIndex(nextIdx)
                .setQuery(query);

        String reqId = UUID.randomUUID().toString();
        String nextId = UUID.randomUUID().toString();
        myCallbacks.put(reqId, req1);
        myCallbacks.put(nextId, next);

        return new String[]{reqId, nextId};
    }

    private void sendError(Long chat, String text) {
        SendMessage na = new SendMessage()
                .setChatId(chat)
                .setText(text);
        sendMessage(na);
    }

    private void sendMissing(Long chat) {
        SendMessage na = new SendMessage()
                .setChatId(chat)
                .setText("I cant remember what I was supposed to do - Either it was already clicked, or you waited too long\nPlease query me again");
        sendMessage(na);
    }

    private void sendSuccessRequest(Long chat) {
        SendMessage na = new SendMessage()
                .setChatId(chat)
                .setText("Request Successful");
        sendMessage(na);
    }

    private void removeMessage(Integer messageid, Long chatid) {
        DeleteMessage deleteMessage = new DeleteMessage()
                .setMessageId(messageid)
                .setChatId(chatid);
        sendMessage(deleteMessage);
    }

    private void sendNoMoreResults(Long chat) {
        SendMessage na = new SendMessage()
                .setChatId(chat)
                .setText("No More Results");
        sendMessage(na);
    }

    private void sendMessage(BotApiMethod na) {
        try {
            mySender.execute(na);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(SendPhoto photo) {
        try {
            mySender.execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


}
