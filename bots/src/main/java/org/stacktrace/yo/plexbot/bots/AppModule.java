package org.stacktrace.yo.plexbot.bots;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.stacktrace.yo.plexbot.service.HttpClient;
import org.telegram.telegrambots.meta.TelegramBotsApi;

public class AppModule {

    private final TelegramBotsApi myTeleApi;
    private final HttpClient myHttpClient;
    private final ObjectMapper myObjectMapper;


    public AppModule(TelegramBotsApi myTeleApi, HttpClient myHttpClient, ObjectMapper myObjectMapper) {
        this.myTeleApi = myTeleApi;
        this.myHttpClient = myHttpClient;
        this.myObjectMapper = myObjectMapper;
    }

    public HttpClient httpClient(){
        return myHttpClient;
    }

    public ObjectMapper objectMapper() {
        return myObjectMapper;
    }

    public TelegramBotsApi tele(){
        return myTeleApi;
    }
}
