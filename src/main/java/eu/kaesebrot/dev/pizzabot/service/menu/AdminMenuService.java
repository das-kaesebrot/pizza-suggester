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

    String CALLBACK_ADMIN_REDEEM = "key-redeem";
    String CALLBACK_ADMIN_CHANGE_PERSONAL_VENUE = "change-personal-venue";
    String CALLBACK_ADMIN_TOGGLE_GLUTENFREE = "toggle-glutenfree";
    String CALLBACK_ADMIN_TOGGLE_LACTOSEFREE = "toggle-lactosefree";
    String CALLBACK_ADMIN_CHANGE_DIET = "change-diet";
    String CALLBACK_ADMIN_ABOUT_ME = "about-me";
    String CALLBACK_ADMIN_FORGET_ME = "forget-me";
    String CALLBACK_ADMIN_GENERATE_KEY = "gen-key";
    String CALLBACK_ADMIN_RESET_INFOPIN = "reset-pin";
    String CALLBACK_ADMIN_BOTSTATS = "botstats";
    String CALLBACK_ADMIN_VENUES_CREATE = VenueEditSubMenuService.CALLBACK_PREFIX + "-" + VenueEditSubMenuService.CALLBACK_CREATE_VENUE;
    String CALLBACK_ADMIN_VENUES_EDIT = VenueEditSubMenuService.CALLBACK_PREFIX + "-" + VenueEditSubMenuService.CALLBACK_ROOT_MENU;

    BotApiMethod<?> handleKeyRedemption(CachedUser user, String key);
    BotApiMethod<?> handleCsvUpload(CachedUser user, String fileId, PizzaSuggesterBot bot) throws TelegramApiException, IOException;
    SendMessage handleVenueCreationReply(CachedUser user, String message);
    SendMessage handleVenueModificationReply(CachedUser user, String message, PizzaSuggesterBot bot);
    SendMessage getAdminMenu(CachedUser user);
}
