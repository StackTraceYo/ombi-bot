package org.stacktrace.yo.plexbot.bots.util;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class Props {

    public static Map<String, String> getProps(String... args) {
        Options options = new Options();

        Option input = new Option("p", "path", true, "input file path");
        input.setRequired(false);
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        String path = null;
        boolean p;
        Map<String, String> props = null;
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
                        if (StringUtils.isNotEmpty(line)) {
                            String trimmed = line.trim();
                            String[] split = trimmed.split("=");
                            if (split.length > 1) {
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
            formatter.printHelp("Plex Bot Runner", options);
            System.exit(1);
        } catch (FileNotFoundException e) {
            log.error("Provided Config Path Not Found {}", path, e);
            System.exit(1);
        } catch (IOException e) {
            log.error("Error Reading Config", e);
            System.exit(1);
        }

        return props;
    }
}
