package eu.kaesebrot.dev.pizzabot.service.menu;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Optional;

public interface IngredientMenuService {
    void regenerateInlineKeyboardPageCache();
    void regenerateInlineKeyboardPageCache(Long venueId);
    InlineKeyboardMarkup getInitialKeyboard(Long venueId);
    InlineKeyboardMarkup getKeyboardForPage(Long venueId, Optional<Integer> pageNumber);
}
