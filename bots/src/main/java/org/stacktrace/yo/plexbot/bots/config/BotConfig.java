package org.stacktrace.yo.plexbot.bots.config;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;


@Data
@Accessors(chain = true)
@AllArgsConstructor
@Slf4j
public class BotConfig {

    public static enum BotConfigName {

        BASE("BASE"),
        OMBI("OMBI"),
        TMDB("TMDB");

        public final String botName;

        BotConfigName(String ombi) {
            this.botName = ombi;
        }
    }


    private final String host;
    private final String key;
    private final String token;
    private final String name;
    private final String username;

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("host", host)
                .append("key", key)
                .append("token", token)
                .append("name", name)
                .append("username", username)
                .toString();
    }

    boolean isValid() {
        return true;
    }

    public static BotConfig base(Map<String, String> props) {
        String teleToken = null;
        String botname = null;

        if (props != null) {
            teleToken = props.get("BOT_TOKEN");
            botname = props.get("BOT_NAME");
        } else {
            teleToken = System.getenv("BOT_TOKEN");
            botname = System.getenv("BOT_NAME");
        }
        return new BotConfig("N/A", "N/A", teleToken, botname, "N/A");

    }

    public static BotConfig tmdb(Map<String, String> props) {
        String apiKey = null;

        if (props != null) {
            apiKey = props.get("TMDB_API_KEY");
        } else {
            apiKey = System.getenv("TMDB_API_KEY");
        }
        return new BotConfig("N/A", apiKey, "N/A", "N/A", "N/A");
    }


    public static BotConfig ombi(Map<String, String> props) {
        String ombiHost = null;
        String ombiKey = null;
        String ombiUsername = null;

        if (props != null) {
            ombiHost = props.get("OMBI_HOST");
            ombiKey = props.get("OMBI_KEY");
            ombiUsername = props.get("OMBI_USER_NAME");
        } else {
            ombiHost = System.getenv("OMBI_HOST");
            ombiKey = System.getenv("OMBI_KEY");
            ombiUsername = System.getenv("OMBI_USER_NAME");

        }
        return new BotConfig(ombiHost, ombiKey, "N/A", "N/A", ombiUsername);
    }


    public static Map<BotConfigName, BotConfig> loadConfigs(Map<String, String> props) {

        Map<BotConfigName, BotConfig> bots = Maps.newHashMap();
        BotConfig base = BotConfig.base(props);
        BotConfig ombi = BotConfig.ombi(props);
        BotConfig tmdb = BotConfig.tmdb(props);

        log.info("[BOT MASTER] LOADED VALUES: {}\n{}\n{}", base, ombi, tmdb);

        if (base.isValid()) {
            log.info("[BOT MASTER] BASE VALID");
            bots.put(BotConfigName.BASE, base);
        } else {
            log.error("Error Loading Bot - Base Config Missing info -> found {}", base);
            System.exit(0);
        }
        if (ombi.isValid()) {
            log.info("[BOT MASTER] OMBI VALID");
            bots.put(BotConfigName.OMBI, ombi);
        }
        if (tmdb.isValid()) {
            log.info("[BOT MASTER] TMDB VALID");
            bots.put(BotConfigName.TMDB, tmdb);
        }
        return bots;
    }


}
