package eu.kaesebrot.dev.pizzabot.classes;

import java.sql.Timestamp;
import java.util.List;

public record IngredientList(List<String> ingredients, Timestamp createdAt) {

}
