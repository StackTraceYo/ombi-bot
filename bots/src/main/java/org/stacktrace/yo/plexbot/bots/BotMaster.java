package org.stacktrace.yo.plexbot.bots;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
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

    public BotMaster(Map<String,String> props) {
        Map<Bots, BotConfig> myBots = load(props);
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

    public Map<Bots, BotConfig> load(Map<String, String> props) {

        Map<Bots, BotConfig> bots = Maps.newHashMap();
        BotConfig ombi = BotConfig.ombi(props);
        log.info("[BOT MASTER] OMBI LOADED VALUES: {}", ombi);
        if (ombi.isValid()) {
            log.info("[BOT MASTER] OMBI VALID");
            bots.put(OMBI, ombi);
        }
        return bots;
    }

    public static void main(String... args) {
        Options options = new Options();

        Option input = new Option("p", "path", true, "input file path");
        input.setRequired(false);
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        String path = null;
        boolean p = false;
        Map<String,String> props = null;
        try {
            cmd = parser.parse(options, args);
            p = cmd.hasOption("p");
            if (p) {
                path = cmd.getOptionValue("p");
                if (StringUtils.isNotEmpty(path)) {
                    props = Maps.newHashMap();
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            new FileInputStream(path), StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if(StringUtils.isNotEmpty(line)){
                            String trimmed = line.trim();
                            String[] split = trimmed.split("=");
                            if(split.length > 1){
                                String key = split[0];
                                String value = split[1];
                                props.put(key, value);
                            }
                        }
                    }
                }
            }
        } catch (ParseException e) {
            log.error("Could Not Parse Arguments", e);
            formatter.printHelp("utility-name", options);
            System.exit(1);
        } catch (FileNotFoundException e) {
            log.error("Provided Config Path Not Found {}", path, e);
            System.exit(1);
        } catch (IOException e) {
            log.error("Error Reading Config {}", e);
            System.exit(1);
        }
        new BotMaster(props);
    }

}
