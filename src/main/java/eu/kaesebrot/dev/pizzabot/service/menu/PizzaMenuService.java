package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface PizzaMenuService extends MenuService {
    int maxIngredientKeyboardRows = 3;
    int maxIngredientKeyboardColumns = 2;
    int MAX_PIZZA_RESULTS = 3;
    String CALLBACK_PREFIX = "pizzamenu";
    String CALLBACK_PIZZA_INGREDIENT_TOGGLE = "ingr";
    SendMessage getRandomPizza(CachedUser user);
    SendMessage getIngredientSelectionMenu(CachedUser user);
}
