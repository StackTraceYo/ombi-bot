package org.stacktrace.yo.plexbot.bots;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.stacktrace.yo.plexbot.bots.capability.Capability;
import org.stacktrace.yo.plexbot.bots.config.BotConfig;
import org.stacktrace.yo.plexbot.bots.config.TestBotConfig;
import org.stacktrace.yo.plexbot.bots.ombi.OmbiCapability;
import org.stacktrace.yo.plexbot.bots.tmdb.TMDBCapability;
import org.stacktrace.yo.plexbot.bots.util.Props;
import org.stacktrace.yo.plexbot.service.HttpClient;
import org.stacktrace.yo.plexbot.service.ombi.OmbiService;
import org.stacktrace.yo.plexbot.service.api.TMTVDbService;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.logging.BotLogger;
import org.telegram.telegrambots.meta.logging.BotsFileHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import static org.stacktrace.yo.plexbot.bots.config.BotConfig.BotConfigName.*;

@Slf4j
public class BotRunner {

    AppModule myAppModule;
    StackBot myStackBot;

    public static void main(String... args) throws Exception {
        Map<String, String> props = Props.getProps(args);
        Map<BotConfig.BotConfigName, BotConfig> configs = TestBotConfig.loadConfigs(props);
        if (configs.isEmpty()) {
            log.warn("No Config Loaded Exiting...");
            System.exit(1);
        }
        new BotRunner(configs);
    }

    private BotRunner(Map<BotConfig.BotConfigName, BotConfig> configs) throws TelegramApiRequestException {

        myAppModule = createAppModule();
        initializeBots();

        List<Capability> capabilityList = Lists.newArrayList();

        registerOMBICapability(configs.get(OMBI), capabilityList);
        registerTMDBCapability(configs.get(TMDB), configs.get(OMBI), capabilityList);

        myStackBot = new StackBot(configs.get(BASE).getToken(), capabilityList);
        myAppModule.tele().registerBot(myStackBot);
    }

    private void initializeBots() {
        BotLogger.setLevel(Level.ALL);
        BotLogger.registerLogger(new ConsoleHandler());
        try {
            BotLogger.registerLogger(new BotsFileHandler());
        } catch (IOException e) {
            BotLogger.severe("BotMaster", e);
        }
    }

    private AppModule createAppModule() {
        ApiContextInitializer.init();
        ObjectMapper om = new ObjectMapper();
        return new AppModule(
                new TelegramBotsApi(),
                new HttpClient(om),
                om
        );
    }

    private void registerOMBICapability(BotConfig config, List<Capability> capabilityList) {
        if (config != null) {
            OmbiCapability ombiCapability = new OmbiCapability(
                    new OmbiService(myAppModule.httpClient(),
                            new OmbiService.OmbiConfig(config.getHost(), config.getKey(), config.getUsername())
                    )
            );
            capabilityList.add(ombiCapability);
            log.info("Added Ombi Capabilities");
        }
    }

    private void registerTMDBCapability(BotConfig config, BotConfig ombiConfig, List<Capability> capabilityList) {
        if (config != null) {
            TMDBCapability tmdbCapability = new TMDBCapability(
                    new TMTVDbService(myAppModule.httpClient(), new TMTVDbService.TMDBConfig(config.getKey())),
                    new OmbiService(myAppModule.httpClient(),
                            new OmbiService.OmbiConfig(ombiConfig.getHost(), ombiConfig.getKey(), ombiConfig.getUsername())
                    )
            );
            capabilityList.add(tmdbCapability);
            log.info("Added TMDB Capabilities");
        }
    }
}
