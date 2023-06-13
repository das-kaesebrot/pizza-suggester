package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.classes.IngredientList;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;

@Service
public class PizzaServiceImpl implements PizzaService {
    Logger logger = LoggerFactory.getLogger(PizzaServiceImpl.class);
    private final VenueRepository venueRepository;
    private HashMap<Long, IngredientList> venueIngredients;

    public PizzaServiceImpl(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Override
    public List<String> getVenueIngredients(Long venueId) {
        if (!venueRepository.existsById(venueId))
            throw new RuntimeException(String.format("Can't find venue by id %s!", venueId));

        //noinspection OptionalGetWithoutIsPresent
        return getVenueIngredients(venueRepository.findById(venueId).get());
    }

    @Override
    public List<String> getVenueIngredients(Venue venue) {
        generateVenueIngredientCache(venue);

        return venueIngredients.get(venue.getId()).ingredients();
    }

    @Override
    public List<Pizza> getMatchingPizzasByIngredientIndexList(Long venueId, List<Integer> ingredientIndexList) {
        List<String> ingredients = new LinkedList<>();

        for (int ingredientIndex: ingredientIndexList) {
            ingredients.add(resolveIngredientFromIndex(venueId, ingredientIndex));
        }

        return getMatchingPizzasByIngredientStrings(venueId, ingredients);
    }

    @Override
    public List<Pizza> getMatchingPizzasByIngredientStrings(Long venueId, List<String> ingredients) {
        if (!venueRepository.existsById(venueId))
            throw new RuntimeException(String.format("Can't find venue by id %s!", venueId));

        //noinspection OptionalGetWithoutIsPresent
        var venue = venueRepository.findById(venueId).get();

        List<Pizza> matches = new LinkedList<>();

        for (Pizza pizza: venue.getPizzaMenu()) {
            if (pizza.getIngredients().containsAll(ingredients)) {
                matches.add(pizza);
            }
        }

        return matches;
    }

    @Override
    public List<Pizza> filterSortAndTrimListOfPizzasForUser(CachedUser user, List<Pizza> allMatches) {
        return allMatches
                .stream()
                .filter(p -> p.getMinimumUserDiet().ordinal() >= user.getUserDiet().ordinal())
                .sorted(Comparator.comparing(Pizza::getPrice))
                .limit(MAX_PIZZA_RESULTS)
                .toList();
    }

    @Override
    public String resolveIngredientFromIndex(Long venueId, int ingredientIndex) {
        return getVenueIngredients(venueId).get(ingredientIndex);
    }

    @Override
    public Pizza getRandomPizza(Venue venue, CachedUser user) {
        Random rand = new Random();

        var menu = venue.getPizzaMenu();

        while (true) {
            var randPizza = menu.get(rand.nextInt(menu.size()));

            // check if pizza is compatible with user diet before returning
            if (randPizza.getMinimumUserDiet().ordinal() >= user.getUserDiet().ordinal())
                return randPizza;
        }
    }

    private void generateVenueIngredientCache(Venue venue) {
        if ((venueIngredients.containsKey(venue.getId()) || venueIngredients.get(venue.getId()) != null)
            && venueIngredients.get(venue.getId()).createdAt().after(venue.getModifiedAt()))
            return;

        logger.info("Venue has been modified since last cache update, regenerating ingredient cache");

        var frequencyMap = new HashMap<String, Integer>();

        for (Pizza pizza: venue.getPizzaMenu()) {
            var ingredients = pizza.getIngredients();

            for (String ingredient: ingredients) {
                if (!frequencyMap.containsKey(ingredient)) {
                    // first occurrence
                    frequencyMap.put(ingredient, 1);
                }
                else {
                    // increase counter on repeated occurrence
                    frequencyMap.put(ingredient, frequencyMap.get(ingredient) + 1);
                }
            }
        }

        venueIngredients.put(venue.getId(), new IngredientList(sortByComparator(frequencyMap, true), Timestamp.from(Instant.now())));
    }

    private static List<String> sortByComparator(Map<String, Integer> unsortMap, final boolean order)
    {
        List<Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

        // Sorting the list based on values
        list.sort((o1, o2) -> {
            if (order) {
                return o1.getValue().compareTo(o2.getValue());
            } else {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        var returnList = new LinkedList<String>();

        for (var entry: list) {
            returnList.add(entry.getKey());
        }

        return returnList;
    }
}
