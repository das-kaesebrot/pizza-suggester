package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.model.CachedUser;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface AdminService {
    void handleAdminCallback(CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException;
    InlineKeyboardMarkup getAdminMenu(CachedUser user);
}
