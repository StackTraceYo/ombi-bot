package org.stacktrace.yo.plexbot.bots.ombi;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiMovieRequest;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiSearch;
import org.stacktrace.yo.plexbot.models.ombi.request.OmbiTVRequest;
import org.stacktrace.yo.plexbot.models.shared.SearchType;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

public class OmbiBot extends TelegramLongPollingCommandBot {

    private static final Logger myLogger = LoggerFactory.getLogger(OmbiBot.class);
    private final OmbiService myOmbiService;
    private final String myTeleToken;
    private final ObjectMapper myMapper;


    public OmbiBot(OmbiService ombiService, String botname, String token, ObjectMapper mapper) {
        super(botname);
        myOmbiService = ombiService;
        myTeleToken = token;
        myMapper = mapper;
        register(new MovieCommand(myOmbiService));
        register(new TVCommand(myOmbiService));

        registerDefaultAction((absSender, message) -> {
            SendMessage commandUnknownMessage = new SendMessage();
            commandUnknownMessage.setChatId(message.getChatId());
            commandUnknownMessage.setText("The command '" + message.getText() + "' is not known by this bot");
            try {
                absSender.execute(commandUnknownMessage);
            } catch (TelegramApiException e) {
                BotLogger.error("OmbiBot", e);
            }
        });
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

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            if (StringUtils.isNotBlank(query.getData())) {
                try {
                    OmbiCommand.OmbiRequestCallback ombiRequestCallback = myMapper.readValue(query.getData(), OmbiCommand.OmbiRequestCallback.class);
                    if (ombiRequestCallback.getAction().equals("req")) {
                        switch (SearchType.fromReqValue(ombiRequestCallback.getSType())) {
                            case MOVIE:
                                myOmbiService.request(
                                        new OmbiMovieRequest()
                                                .setTheMovieDbId(ombiRequestCallback.getId())
                                ).ifPresent(map1 -> sendSuccessRequest(update.getCallbackQuery().getMessage().getChatId()));
                                break;
                            case TV:
                                myOmbiService.tvDetail(
                                        new OmbiSearch()
                                                .setSearchType(SearchType.TV)
                                                .setDetail(ombiRequestCallback.getId())
                                ).ifPresent(ombiTVDetailResponse -> {
                                    myOmbiService.request(
                                            new OmbiTVRequest()
                                                    .setTvDbId(ombiRequestCallback.getId())
                                                    .setRequestAll(true)
                                                    .setSeasons(ombiTVDetailResponse.getSeasonRequests())
                                    ).ifPresent(map1 -> sendSuccessRequest(update.getCallbackQuery().getMessage().getChatId()));
                                });
                                break;
                            case NA:
                                sendError(update.getMessage().getChatId());
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
