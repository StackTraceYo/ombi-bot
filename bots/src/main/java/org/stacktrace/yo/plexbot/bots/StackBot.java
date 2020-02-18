package org.stacktrace.yo.plexbot.bots;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.capability.CallbackHandler;
import org.stacktrace.yo.plexbot.bots.capability.Capability;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.logging.BotLogger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class StackBot extends TelegramLongPollingCommandBot {

    private final String myTeleToken;
    private final ScheduledExecutorService myScheduledExecutorService;
    private final List<CallbackHandler> myHandlers = Lists.newArrayList();

    public StackBot(String token, List<Capability> capabilities) {
        super("Plex-Stack-Bot");
        myTeleToken = token;
        capabilities.forEach(c -> {
            CallbackHandler handler = c.handler(this);
            myHandlers.add(handler);
            List<IBotCommand> commands = c.commands(handler);
            commands.forEach(this::register);
        });

        registerDefaultAction((absSender, message) -> {
            SendMessage commandUnknownMessage = new SendMessage();
            commandUnknownMessage.setChatId(message.getChatId());
            commandUnknownMessage.setText("The command '" + message.getText() + "' is not known by this bot\n Use /info to see available commands");
            try {
                absSender.execute(commandUnknownMessage);
            } catch (TelegramApiException e) {
                BotLogger.error("StackBot", e);
            }
        });
        myScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        myScheduledExecutorService.scheduleAtFixedRate(() -> {
            log.warn("Clearing Callbacks");
            myHandlers.forEach(CallbackHandler::clear);
        }, 0, 30, TimeUnit.MINUTES);
    }


    @Override
    public String getBotToken() {
        return myTeleToken;
    }


    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            myHandlers.forEach(h -> h.doCallBack(update));
        }
    }
}
