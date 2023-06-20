package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public interface AdminMenuService extends MenuService {
    String CALLBACK_PREFIX = "admin";
    int MENU_COLUMNS = 1;
    int MENU_ROWS = 3;
    String CALLBACK_ADMIN_VENUES_CREATE = VenueEditSubMenuService.CALLBACK_PREFIX + "-" + VenueEditSubMenuService.CALLBACK_CREATE_VENUE;
    String CALLBACK_ADMIN_VENUES_EDIT = VenueEditSubMenuService.CALLBACK_PREFIX + "-" + VenueEditSubMenuService.CALLBACK_ROOT_MENU;

    BotApiMethod<?> handleKeyRedemption(CachedUser user, String key);
    BotApiMethod<?> handleCsvUpload(CachedUser user, String fileId, PizzaSuggesterBot bot) throws TelegramApiException, IOException;
    SendMessage handleVenueCreationReply(CachedUser user, String message);
    SendMessage handleVenueModificationReply(CachedUser user, String message, PizzaSuggesterBot bot);
    SendMessage getAdminMenu(CachedUser user);
}
