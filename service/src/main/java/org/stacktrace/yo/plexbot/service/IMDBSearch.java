package org.stacktrace.yo.plexbot.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Slf4j
public class IMDBSearch {

    private final String myUrl;

    public IMDBSearch(String imdbUrl) {
        this.myUrl = imdbUrl;
    }

    public String getTitle() {
        try {
            Document doc = Jsoup.connect(myUrl).get();
            String title = doc.title();
            // hack
            int i = title.indexOf("(");
            return title.substring(0, i).trim();
        } catch (Exception e) {
            return "";
        }

    }


}
