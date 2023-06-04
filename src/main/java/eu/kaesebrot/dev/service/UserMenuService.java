package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface UserMenuService {
    String CALLBACK_PREFIX = "usermenu";
    BotApiMethod<?> handleUserMenuCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException;
    SendMessage getDietSelection(CachedUser user);
    SendMessage getVenueSelection(CachedUser user);
}
