package org.stacktrace.yo.plexbot.bots.ombi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiMovieRequest;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiTVRequest;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiMovieSearchResponse;
import org.stacktrace.yo.plexbot.models.ombi.response.OmbiTVSearchResponse;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.stacktrace.yo.plexbot.bots.ombi.OmbiCommand.requestSearch;
import static org.stacktrace.yo.plexbot.bots.ombi.OmbiCommand.requestSearchNoPhoto;

@Slf4j
public class OmbiBot extends TelegramLongPollingCommandBot {

    private final OmbiService myOmbiService;
    private final String myTeleToken;
    private final ObjectMapper myMapper;
    private final HashMap<String, OmbiCommand.OmbiRequestCallback> callbacks;
    private final ScheduledExecutorService myScheduledExecutorService;

    public OmbiBot(OmbiService ombiService, String botname, String token, ObjectMapper mapper) {
        super(botname);
        myOmbiService = ombiService;
        myTeleToken = token;
        myMapper = mapper;
        register(new MovieCommand(this, myOmbiService));
        register(new TVCommand(this, myOmbiService));
        register(new InfoCommand());

        registerDefaultAction((absSender, message) -> {
            SendMessage commandUnknownMessage = new SendMessage();
            commandUnknownMessage.setChatId(message.getChatId());
            commandUnknownMessage.setText("The command '" + message.getText() + "' is not known by this bot\n Use /info to see available commands");
            try {
                absSender.execute(commandUnknownMessage);
            } catch (TelegramApiException e) {
                BotLogger.error("OmbiBot", e);
            }
        });
        callbacks = new HashMap<>();
        myScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        myScheduledExecutorService.scheduleAtFixedRate(() -> {
            log.warn("Clearing Callbacks");
            callbacks.clear();
        }, 0, 30, TimeUnit.MINUTES);
    }


    @Override
    public String getBotToken() {
        return myTeleToken;
    }

    private void sendSuccessRequest(Long chat) {
        SendMessage na = new SendMessage()
                .setChatId(chat)
                .setText("Request Successful");
        try {
            execute(na);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private void sendNoMoreResults(Long chat) {
        SendMessage na = new SendMessage()
                .setChatId(chat)
                .setText("No More Results");
        try {
            execute(na);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    private void sendError(Long chat) {
        SendMessage na = new SendMessage()
                .setChatId(chat)
                .setText("Failed To Request");
        try {
            execute(na);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMissing(Long chat) {
        SendMessage na = new SendMessage()
                .setChatId(chat)
                .setText("I cant remember what I was supposed to do - Either it was already clicked, or you waited too long\nPlease query me again");
        try {
            execute(na);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            if (StringUtils.isNotBlank(query.getData())) {
                try {
                    String cbId = query.getData();
                    OmbiCommand.OmbiRequestCallback cb = callbacks.get(cbId);
                    callbacks.remove(cbId);
                    if (null == cb) {
                        sendMissing(update.getMessage().getChatId());
                    } else if (cb.getAction().equals("req")) {
                        switch (SearchType.fromReqValue(cb.getSType())) {
                            case MOVIE:
                                myOmbiService.request(
                                        new OmbiMovieRequest()
                                                .setTheMovieDbId(cb.getId())
                                ).ifPresent(map1 -> sendSuccessRequest(update.getCallbackQuery().getMessage().getChatId()));
                                break;
                            case TV:
                                myOmbiService.tvDetail(
                                        new OmbiSearch()
                                                .setSearchType(SearchType.TV)
                                                .setDetail(cb.getId())
                                ).ifPresent(ombiTVDetailResponse -> {
                                    myOmbiService.request(
                                            new OmbiTVRequest()
                                                    .setTvDbId(cb.getId())
                                                    .setRequestAll(true)
                                                    .setSeasons(ombiTVDetailResponse.getSeasonRequests())
                                    ).ifPresent(map1 -> sendSuccessRequest(update.getCallbackQuery().getMessage().getChatId()));
                                });
                                break;
                            case NA:
                                sendError(update.getMessage().getChatId());
                                break;
                        }
                    } else if (cb.getAction().equals("next")) {
                        next(update.getCallbackQuery().getMessage().getChatId(), cb);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void next(Long chatId, OmbiCommand.OmbiRequestCallback cb) {
        try {

            int nextIdx = cb.getIndex() + 1;
            switch (SearchType.fromReqValue(cb.getSType())) {
                case MOVIE:
                    List<OmbiMovieSearchResponse> searchResultsMovie = myOmbiService.movieSearch(
                            new OmbiSearch()
                                    .setSearchType(SearchType.MOVIE)
                                    .setQuery(cb.getQuery())
                    );

                    if (nextIdx >= searchResultsMovie.size()) {
                        sendNoMoreResults(chatId);
                    } else {
                        OmbiMovieSearchResponse result = searchResultsMovie.get(nextIdx);
                        OmbiCommand.OmbiRequestCallback req1 = new OmbiCommand.OmbiRequestCallback().setAction("req")
                                .setSType(result.getType().getReqValue())
                                .setId(result.getId());

                        OmbiCommand.OmbiRequestCallback next = new OmbiCommand.OmbiRequestCallback().setAction("next")
                                .setSType(result.getType().getReqValue())
                                .setIndex(nextIdx)
                                .setQuery(cb.getQuery());

                        String reqId = UUID.randomUUID().toString();
                        String nextId = UUID.randomUUID().toString();
                        callbacks.put(reqId, req1);
                        callbacks.put(nextId, next);

                        if (result.getPosterPath() != null) {
                            SendPhoto sendPhoto = requestSearch(chatId, reqId, nextId, result);
                            execute(sendPhoto);
                        } else {
                            execute(requestSearchNoPhoto(chatId, reqId, nextId, result));
                        }
                    }
                    break;
                case TV:
                    List<OmbiTVSearchResponse> searchResultsTv = myOmbiService.tvSearch(
                            new OmbiSearch()
                                    .setSearchType(SearchType.TV)
                                    .setQuery(cb.getQuery())
                    );
                    if (nextIdx >= searchResultsTv.size()) {
                        sendNoMoreResults(chatId);
                    } else {
                        OmbiTVSearchResponse result = searchResultsTv.get(nextIdx);

                        OmbiCommand.OmbiRequestCallback req1 = new OmbiCommand.OmbiRequestCallback().setAction("req")
                                .setSType(result.getType().getReqValue())
                                .setId(result.getId());

                        OmbiCommand.OmbiRequestCallback next = new OmbiCommand.OmbiRequestCallback().setAction("next")
                                .setSType(result.getType().getReqValue())
                                .setIndex(nextIdx)
                                .setQuery(cb.getQuery());

                        String reqId = UUID.randomUUID().toString();
                        String nextId = UUID.randomUUID().toString();
                        callbacks.put(reqId, req1);
                        callbacks.put(nextId, next);


                        if (result.photoPath() != null) {
                            SendPhoto sendPhoto = requestSearch(chatId, reqId, nextId, result);
                            execute(sendPhoto);
                        } else {
                            execute(requestSearchNoPhoto(chatId, reqId, nextId, result));
                        }

                    }
                    break;
            }
        } catch (Exception e) {
            sendError(chatId);
            e.printStackTrace();
        }
    }

    public void addCallback(String id, OmbiCommand.OmbiRequestCallback cb) {
        callbacks.put(id, cb);
    }
}
