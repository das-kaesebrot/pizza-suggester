package eu.kaesebrot.dev.service.menu;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public interface AdminMenuService extends MenuService {
    String CALLBACK_PREFIX = "admin";
    BotApiMethod<?> handleKeyRedemption(CachedUser user, String key);
    BotApiMethod<?> handleCsvUpload(CachedUser user, String fileId, PizzaSuggesterBot bot) throws TelegramApiException, IOException;
    InlineKeyboardMarkup getAdminMenu(CachedUser user);
}
