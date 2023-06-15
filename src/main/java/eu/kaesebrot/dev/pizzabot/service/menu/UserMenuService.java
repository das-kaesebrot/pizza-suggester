package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface UserMenuService extends MenuService {
    String CALLBACK_PREFIX = "usermenu";
    SendMessage getDietSelection(CachedUser user);
    SendMessage getVenueSelection(CachedUser user);
    SendMessage getRandomPizza(CachedUser user);
    SendMessage getHelpMessageWithGreeting(CachedUser user);
    SendMessage getHelpMessage(CachedUser user);
    SendMessage getAboutMessage(CachedUser user);
    SendMessage getIngredientSelectionMenu(CachedUser user);
}
