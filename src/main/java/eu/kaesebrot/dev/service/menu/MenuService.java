package eu.kaesebrot.dev.service.menu;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface MenuService {
    BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException;
    boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query);
}
