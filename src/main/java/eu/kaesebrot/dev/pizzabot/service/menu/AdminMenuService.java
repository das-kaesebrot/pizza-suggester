package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public interface AdminMenuService extends MenuService {
    String CALLBACK_PREFIX = "admin";
    public final Long MENU_COLUMNS = 1L;
    public final Long MENU_ROWS = 3L;

    BotApiMethod<?> handleKeyRedemption(CachedUser user, String key);
    BotApiMethod<?> handleCsvUpload(CachedUser user, String fileId, PizzaSuggesterBot bot) throws TelegramApiException, IOException;
    InlineKeyboardMarkup getAdminMenu(CachedUser user);
    InlineKeyboardMarkup getAdminMenu(CachedUser user, long zeroBasedPage);
}
