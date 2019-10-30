package org.stacktrace.yo.plexbot.bots.ombi;

import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class OmbiBot extends TelegramLongPollingCommandBot {

    private final String myTeleToken;
    private final ScheduledExecutorService myScheduledExecutorService;
    private final OmbiCallbackHandler myCallBackHandler;

    public OmbiBot(OmbiService ombiService, String botname, String token) {
        super(botname);
        myTeleToken = token;
        myCallBackHandler = new OmbiCallbackHandler(ombiService, this);

        register(new MovieCommand(ombiService, myCallBackHandler));
        register(new TVCommand(ombiService, myCallBackHandler));
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
        myScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        myScheduledExecutorService.scheduleAtFixedRate(() -> {
            log.warn("Clearing Callbacks");
            myCallBackHandler.clearCallbacks();
        }, 0, 30, TimeUnit.MINUTES);
    }


    @Override
    public String getBotToken() {
        return myTeleToken;
    }


    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            myCallBackHandler.doCallBack(update);
        }
    }
}
