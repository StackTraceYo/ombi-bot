package org.stacktrace.yo.plexbot.bots;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.ombi.OmbiBot;
import org.stacktrace.yo.plexbot.service.HttpClient;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.logging.BotLogger;
import org.telegram.telegrambots.meta.logging.BotsFileHandler;

import static org.stacktrace.yo.plexbot.bots.Bots.OMBI;

@Slf4j
public class BotMaster {

    private final TelegramBotsApi myTeleApi;
    private HttpClient myHttpClient;
    private ObjectMapper myObjectMapper;

    public BotMaster() {
        Map<Bots, BotConfig> myBots = load();
        if (myBots.isEmpty()) {
            log.warn("No Bots Loaded Exiting...");
            myTeleApi = null;
            System.exit(1);
        } else {
            BotLogger.setLevel(Level.ALL);
            BotLogger.registerLogger(new ConsoleHandler());
            try {
                BotLogger.registerLogger(new BotsFileHandler());
            } catch (IOException e) {
                BotLogger.severe("BotMaster", e);
            }
            ApiContextInitializer.init();
            myTeleApi = new TelegramBotsApi();
            myObjectMapper = new ObjectMapper();
            myHttpClient = new HttpClient(myObjectMapper);

            myBots.forEach((s, botConfig) -> {
                try {
                    switch (s) {
                        case OMBI:
                            OmbiBot ombiBot = new OmbiBot(
                                    new OmbiService(myHttpClient, new OmbiService.OmbiConfig(
                                            botConfig.getHost(),
                                            botConfig.getKey())
                                    ),
                                    botConfig.getName(),
                                    botConfig.getToken(),
                                    myObjectMapper
                            );
                            log.warn("Loading Ombibot {}", s);
                            myTeleApi.registerBot(ombiBot);
                            break;
                        default:
                            log.warn("Unknown Bot Name {}", s);
                    }
                } catch (TelegramApiRequestException e) {
                    log.error("Error Loading Bot {}", s, e);
                    System.exit(1);
                }
            });
        }
    }

    public Map<Bots, BotConfig> load() {

        Map<Bots, BotConfig> bots = Maps.newHashMap();
        BotConfig ombi = BotConfig.ombi();
        log.info("[BOT MASTER] OMBI LOADED VALUES: {}", ombi);
        if (ombi.isValid()) {
            log.info("[BOT MASTER] OMBI VALID");
            bots.put(OMBI, ombi);
        }
        return bots;
    }

    public static void main(String... args) {
        new BotMaster();
    }

}
