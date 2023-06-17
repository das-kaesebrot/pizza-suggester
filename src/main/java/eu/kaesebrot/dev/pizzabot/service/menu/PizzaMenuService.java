package eu.kaesebrot.dev.pizzabot.service.menu;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Optional;

public interface PizzaMenuService extends MenuService {
    int maxIngredientKeyboardRows = 3;
    int maxIngredientKeyboardColumns = 2;
    String CALLBACK_PREFIX = "pizzamenu";
    InlineKeyboardMarkup getInitialKeyboard(Long venueId);
    InlineKeyboardMarkup getKeyboardForPage(Long venueId, Optional<Integer> pageNumber);
}
