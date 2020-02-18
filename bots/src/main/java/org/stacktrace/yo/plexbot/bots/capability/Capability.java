package org.stacktrace.yo.plexbot.bots.capability;

import org.telegram.telegrambots.extensions.bots.commandbot.commands.IBotCommand;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.List;

public interface Capability<T extends CallbackHandler> {

    T handler(AbsSender sender);

    List<IBotCommand> commands(T handler);

    String capabilityName();

    String info();
}
