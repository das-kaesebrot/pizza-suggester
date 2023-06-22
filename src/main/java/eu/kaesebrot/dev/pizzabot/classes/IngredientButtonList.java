package eu.kaesebrot.dev.pizzabot.classes;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.sql.Timestamp;
import java.util.List;

public record IngredientButtonList(List<List<List<InlineKeyboardButton>>> ingredientButtons, Timestamp createdAt) {

}
