package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;

public interface VenueEditSubMenuService extends MenuService {
    int MENU_COLUMNS = 1;
    int MENU_ROWS = 3;
    int EDIT_MENU_COLUMNS = 2;
    int EDIT_MENU_ROWS = 5;
    public final String CALLBACK_PREFIX = "v";
    // only used to get the submenu for editing venues from the admin menu
    public final String CALLBACK_ROOT_MENU = "menu";
    // only used to initialize the venue creation process
    public final String CALLBACK_CREATE_VENUE = "create";
    // used in venue selection menu before editing a venue (overview of all venues to edit)
    public final String CALLBACK_SUBMENU_PREFIX = "edit";

    // used for buttons when selecting an edit operation for a venue
    public final String CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX = CALLBACK_SUBMENU_PREFIX + "-ops";
    // used for editing the name
    public final String CALLBACK_EDIT_NAME = CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX + "-n";
    public final String CALLBACK_EDIT_ADDRESS = CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX + "-a";
    public final String CALLBACK_EDIT_URL = CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX + "-u";
    public final String CALLBACK_EDIT_PHONE = CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX + "-p";
    public final String CALLBACK_EDIT_PIZZAS = CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX + "-c";
    public final String CALLBACK_DELETE = CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX + "-d";

    BotApiMethod<?> handleCsvUpload(CachedUser user, String fileId, PizzaSuggesterBot bot) throws TelegramApiException, IOException;
    SendMessage handleVenueCreationReply(CachedUser user, String message);
    SendMessage handleVenueModificationReply(CachedUser user, String message, PizzaSuggesterBot bot);
}
