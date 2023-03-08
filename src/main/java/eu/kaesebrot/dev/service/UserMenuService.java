package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.model.CachedUser;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface UserMenuService {
    String CALLBACK_PREFIX = "usermenu";
    void handleUserMenuCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException;
    InlineKeyboardMarkup getDietSelection(CachedUser user);
    InlineKeyboardMarkup getVenueSelection(CachedUser user);
}
