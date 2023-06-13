package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;

import java.util.List;

public interface PizzaService {
    int MAX_PIZZA_RESULTS = 3;
    List<String> getVenueIngredients(Long venueId);
    List<String> getVenueIngredients(Venue venue);
    List<Pizza> getMatchingPizzasByIngredientIndexList(Long venueId, List<Integer> ingredientIndexList);
    List<Pizza> getMatchingPizzasByIngredientStrings(Long venueId, List<String> ingredients);
    List<Pizza> filterSortAndTrimListOfPizzasForUser(CachedUser user, List<Pizza> allMatches);
    String resolveIngredientFromIndex(Long venueId, int ingredientIndex);
    Pizza getRandomPizza(Venue venue, CachedUser user);
}
