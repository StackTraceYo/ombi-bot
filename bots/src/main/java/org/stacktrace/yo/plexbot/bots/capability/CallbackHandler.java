package org.stacktrace.yo.plexbot.bots.capability;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface CallbackHandler {

    void clear();

    void doCallBack(Update update);
}

