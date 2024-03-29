package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.classes.IngredientList;
import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;

import java.util.List;

public interface PizzaService {
    List<String> getVenueIngredients(Long venueId);
    List<String> getVenueIngredients(Venue venue);
    IngredientList getVenueIngredientList(Venue venue);
    List<Pizza> getMatchingPizzasByIngredientIndexList(Long venueId, UserDiet diet, List<Integer> ingredientIndexList);
    List<Pizza> getMatchingPizzasByIngredientStrings(Long venueId, UserDiet diet, List<String> ingredients);
    List<Pizza> filterSortAndTrimListOfPizzasForUser(CachedUser user, List<Pizza> allMatches, int maxResults);
    String resolveIngredientFromIndex(Long venueId, int ingredientIndex);
    Pizza getRandomPizza(Venue venue, CachedUser user);
}
