package eu.kaesebrot.dev.handler;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface IUpdateHandler {
    BotApiMethod handleUpdate(Update update);
}
