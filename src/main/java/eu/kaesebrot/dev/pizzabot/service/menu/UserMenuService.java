package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface UserMenuService extends MenuService {
    String CALLBACK_PREFIX = "usermenu";
    String CALLBACK_DIET_PREFIX = "diet";
    String CALLBACK_VENUE_PREFIX = "venue";
    String CALLBACK_VENUE_SELECTION = "select";
    String MESSAGES_LABEL_DIET_PREFIX = "label.diet";
    int VENUE_SELECTION_COLUMNS = 1;
    int VENUE_SELECTION_ROWS = 3;
    SendMessage getDietSelection(CachedUser user);
    SendMessage getVenueSelection(CachedUser user);
    SendMessage getSetupMessages(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException;
    SendMessage getHelpMessageWithGreeting(CachedUser user);
    SendMessage getHelpMessage(CachedUser user);
    SendMessage getAboutMessage(CachedUser user);
    BotApiMethod<?> getVenueContactInfoMessages(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException;
    void resetPinnedInfoMessage(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException;
}
