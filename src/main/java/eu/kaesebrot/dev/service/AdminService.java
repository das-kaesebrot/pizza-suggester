package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.model.CachedUser;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface AdminService {
    String CALLBACK_PREFIX = "admin";
    void handleAdminCallback(CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException;
    void handleKeyRedemption(CachedUser user, String key);
    InlineKeyboardMarkup getAdminMenu(CachedUser user);
}
