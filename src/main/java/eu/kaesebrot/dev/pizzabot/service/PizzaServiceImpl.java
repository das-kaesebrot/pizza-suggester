package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.classes.IngredientList;
import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.exceptions.NoPizzasFoundException;
import eu.kaesebrot.dev.pizzabot.exceptions.NoPizzasYetForVenueException;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import eu.kaesebrot.dev.pizzabot.repository.PizzaRepository;
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
    private final PizzaRepository pizzaRepository;
    private HashMap<Long, IngredientList> venueIngredients = new HashMap<>();

    public PizzaServiceImpl(VenueRepository venueRepository, PizzaRepository pizzaRepository) {
        this.venueRepository = venueRepository;
        this.pizzaRepository = pizzaRepository;
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
    public IngredientList getVenueIngredientList(Venue venue) {
        generateVenueIngredientCache(venue);

        return venueIngredients.get(venue.getId());
    }

    @Override
    public List<Pizza> getMatchingPizzasByIngredientIndexList(Long venueId, UserDiet diet, List<Integer> ingredientIndexList) {
        List<String> ingredients = new LinkedList<>();

        for (int ingredientIndex: ingredientIndexList) {
            ingredients.add(resolveIngredientFromIndex(venueId, ingredientIndex));
        }

        return getMatchingPizzasByIngredientStrings(venueId, diet, ingredients);
    }

    @Override
    public List<Pizza> getMatchingPizzasByIngredientStrings(Long venueId, UserDiet diet, List<String> ingredients) {
        if (!venueRepository.existsById(venueId))
            throw new RuntimeException(String.format("Can't find venue by id %s!", venueId));

        //noinspection OptionalGetWithoutIsPresent
        var menu = pizzaRepository.findByVenueAndMinimumUserDietGreaterThanEqual(venueRepository.findById(venueId).get(), diet);

        List<Pizza> matches = new LinkedList<>();

        for (Pizza pizza: menu) {
            if (new HashSet<>(pizza.getIngredients()).containsAll(ingredients)) {
                matches.add(pizza);
            }
        }

        return matches;
    }

    @Override
    public List<Pizza> filterSortAndTrimListOfPizzasForUser(CachedUser user, List<Pizza> allMatches, int maxResults) {

        if (user.isGlutenIntolerant() && user.getSelectedVenue().supportsGlutenFree())
            allMatches.forEach(p -> p.setPrice(p.getPrice().add(user.getSelectedVenue().getGlutenFreeMarkup())));

        if (user.isLactoseIntolerant() && user.getSelectedVenue().supportsLactoseFree())
            allMatches.forEach(p -> p.setPrice(p.getPrice().add(user.getSelectedVenue().getLactoseFreeMarkup())));

        return allMatches
                .stream()
                .filter(p -> p.getMinimumUserDiet().ordinal() >= user.getUserDiet().ordinal())
                .sorted(Comparator.comparing(Pizza::getPrice))
                .limit(maxResults)
                .toList();
    }

    @Override
    public String resolveIngredientFromIndex(Long venueId, int ingredientIndex) {
        return getVenueIngredients(venueId).get(ingredientIndex);
    }

    @Override
    public Pizza getRandomPizza(Venue venue, CachedUser user) {
        Random rand = new Random();

        if (!pizzaRepository.existsPizzasByVenue(user.getSelectedVenue()))
            throw new NoPizzasYetForVenueException(venue);

        var menu = pizzaRepository.findByVenueAndMinimumUserDietGreaterThanEqual(venue, user.getUserDiet());

        if (menu.isEmpty()
                || (!venue.supportsGlutenFree() && user.isGlutenIntolerant())
                || (!venue.supportsLactoseFree() && user.isLactoseIntolerant()))
            throw new NoPizzasFoundException();

        while (true) {
            var randPizza = menu.get(rand.nextInt(menu.size()));

            // check if pizza is compatible with user diet before returning
            if (randPizza.getMinimumUserDiet().ordinal() >= user.getUserDiet().ordinal()) {

                if (user.isGlutenIntolerant())
                    randPizza.setPrice(randPizza.getPrice().add(venue.getGlutenFreeMarkup()));

                if (user.isLactoseIntolerant())
                    randPizza.setPrice(randPizza.getPrice().add(venue.getLactoseFreeMarkup()));

                return randPizza;
            }
        }
    }

    private void generateVenueIngredientCache(Venue venue) {
        if ((venueIngredients.containsKey(venue.getId()) || venueIngredients.get(venue.getId()) != null)
            && venueIngredients.get(venue.getId()).createdAt().after(venue.getModifiedAt()))
            return;

        logger.info("Venue has been modified since last cache update, regenerating ingredient cache");

        var frequencyMap = new HashMap<String, Integer>();

        for (Pizza pizza: pizzaRepository.findByVenue(venue)) {
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

        venueIngredients.put(venue.getId(), new IngredientList(sortByComparator(frequencyMap, false), Timestamp.from(Instant.now())));
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
