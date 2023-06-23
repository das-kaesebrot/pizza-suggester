package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.classes.IngredientButtonList;
import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.exceptions.PendingVenueSelectionException;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import eu.kaesebrot.dev.pizzabot.properties.TelegramBotProperties;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import eu.kaesebrot.dev.pizzabot.service.InlineKeyboardService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.service.PizzaService;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;

@Service
public class PizzaMenuServiceImpl implements PizzaMenuService {
    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final InlineKeyboardService inlineKeyboardService;
    private final PizzaService pizzaService;
    private final LocalizationService localizationService;
    private final TelegramBotProperties botProperties;
    private HashMap<Long, IngredientButtonList> venueIngredientButtons = new HashMap<>();

    public PizzaMenuServiceImpl(VenueRepository venueRepository, CachedUserRepository cachedUserRepository, InlineKeyboardService inlineKeyboardService, PizzaService pizzaService, LocalizationService localizationService, TelegramBotProperties botProperties) {
        this.venueRepository = venueRepository;
        this.cachedUserRepository = cachedUserRepository;
        this.inlineKeyboardService = inlineKeyboardService;
        this.pizzaService = pizzaService;
        this.localizationService = localizationService;
        this.botProperties = botProperties;
    }

    @Override
    public String getCallbackPrefix() {
        return CALLBACK_PREFIX;
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {

        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        var sanitizedData = stripCallbackPrefix(query.getData());
        var sanitizedDataWithoutNumbers = StringUtils.stripNumberFromCallbackData(sanitizedData);
        var number = StringUtils.getNumberFromCallbackData(sanitizedData);

        switch (sanitizedDataWithoutNumbers) {
            case CALLBACK_PIZZA_INGREDIENT_TOGGLE:
                toggleSelectedIngredient(user, number);

                var editMessageText = new EditMessageText();
                editMessageText.setText(getMessageStringForIngredientToggle(user));
                editMessageText.setChatId(user.getChatId().toString());
                editMessageText.setMessageId(query.getMessage().getMessageId());
                editMessageText.setReplyMarkup(getKeyboardForPage(user.getSelectedVenue(), user.getCurrentIngredientMenuPage()));

                bot.execute(editMessageText);
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_CONFIRM:
                user.setCurrentIngredientMenuPage(0);
                cachedUserRepository.save(user);
                bot.execute(handleButtonPressConfirmIngredients(user));
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
                reply.setText(localizationService.getString("admin.pagepress"));
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
                var editMessageMarkup = new EditMessageReplyMarkup();
                editMessageMarkup.setChatId(user.getChatId().toString());
                editMessageMarkup.setMessageId(query.getMessage().getMessageId());
                editMessageMarkup.setReplyMarkup(getKeyboardForPage(user.getSelectedVenue(), number));

                user.setCurrentIngredientMenuPage(number);
                cachedUserRepository.save(user);

                bot.execute(editMessageMarkup);
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_CLOSE:
                user.setCurrentIngredientMenuPage(0);
                cachedUserRepository.save(user);
                break;

            default:
                throw new RuntimeException(String.format("Unknown callback data for pizza menu! Given data: %s", query.getData()));
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        var sanitizedData = StringUtils.stripNumberFromCallbackData(stripCallbackPrefix(query.getData()));

        switch (sanitizedData) {
            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
            case CALLBACK_PIZZA_INGREDIENT_TOGGLE:
                return false;

            default:
                return true;
        }
    }

    @Override
    public SendMessage getRandomPizza(CachedUser user) {
        if (user.getSelectedVenue() == null)
            throw new PendingVenueSelectionException("No venue selected by user yet!");

        var pizza = pizzaService.getRandomPizza(user.getSelectedVenue(), user);

        var text = formatPizzaForMessage(user, pizza);
        var formattedPizzaRandText = localizationService.getString("pizza.random");
        text = StringUtils.replacePropertiesVariable("pizza_info", text, formattedPizzaRandText);
        text = StringUtils.replacePropertiesVariable("greeting", StringUtils.escapeForMarkdownV2Format(localizationService.getString("pizza.greeting")), text);

        SendMessage reply = new SendMessage(user.getChatId().toString(), text);
        reply.setParseMode(ParseMode.MARKDOWNV2);

        return reply;
    }

    @Override
    public SendMessage getIngredientSelectionMenu(CachedUser user) {
        if (user.getSelectedVenue() == null)
            throw new PendingVenueSelectionException("No venue selected by user yet!");

        // initialize ingredient selection array
        user.clearSelectedIngredients();
        user.setCurrentIngredientMenuPage(0);

        cachedUserRepository.save(user);

        SendMessage reply = new SendMessage(user.getChatId().toString(), localizationService.getString("pizza.selectingredients"));
        reply.setParseMode(ParseMode.MARKDOWNV2);

        reply.setReplyMarkup(getKeyboardForPage(user.getSelectedVenue(), 0));

        return reply;
    }

    private SendMessage handleButtonPressConfirmIngredients(CachedUser user) {
        var reply =  new SendMessage();
        reply.setChatId(user.getChatId());

        if ((!user.getSelectedVenue().supportsGlutenFree() && user.isGlutenIntolerant())
            || (!user.getSelectedVenue().supportsLactoseFree() && user.isLactoseIntolerant())) {
            reply.setText(localizationService.getString("error.nopizzasfound"));
            return reply;
        }

        var allMatchingPizzas = pizzaService.getMatchingPizzasByIngredientIndexList(user.getSelectedVenue().getId(), user.getUserDiet(), user.getSelectedUserIngredients());
        allMatchingPizzas = pizzaService.filterSortAndTrimListOfPizzasForUser(user, allMatchingPizzas, MAX_PIZZA_RESULTS);

        if (allMatchingPizzas.isEmpty()) {
            // throw new NoPizzasFoundException();
            reply.setText(localizationService.getString("error.nopizzasfoundwithretry"));
            return reply;
        }

        var formattedPizzaStrings = new StringBuilder();

        for (int index = 0; index < allMatchingPizzas.size(); index++) {
            formattedPizzaStrings
                    .append(formatPizzaForMessage(user, allMatchingPizzas.get(index)));

            if (index != allMatchingPizzas.size() - 1) {
                formattedPizzaStrings.append("\n\n");
            }

        }

        var text = localizationService.getString("pizza.selection");
        text = StringUtils.replacePropertiesVariable("pizza_info_list", formattedPizzaStrings.toString(), text);
        text = StringUtils.replacePropertiesVariable("greeting", StringUtils.escapeForMarkdownV2Format(localizationService.getString("pizza.greeting")), text);

        reply.setText(text);
        reply.setParseMode(ParseMode.MARKDOWNV2);

        user.clearSelectedIngredients();

        cachedUserRepository.save(user);

        return reply;
    }

    private InlineKeyboardMarkup getKeyboardForPage(Venue venue, int pageNumber) {
        regenerateInlineKeyboardPageCache();

        return new InlineKeyboardMarkup(venueIngredientButtons.get(venue.getId()).ingredientButtons().get(pageNumber));
    }

    private void regenerateInlineKeyboardPageCache() {
        for (var venue : venueRepository.findAll()) {
            if ((venueIngredientButtons.containsKey(venue.getId()) || venueIngredientButtons.get(venue.getId()) != null)
                    && venueIngredientButtons.get(venue.getId()).createdAt().after(venue.getModifiedAt())) {
                continue;
            }

            var ingredientList = pizzaService.getVenueIngredientList(venue);

            if (ingredientList.ingredients().isEmpty())
                continue;

            var listOfIngredientButtons = new ArrayList<InlineKeyboardButton>();

            for (int index = 0; index < ingredientList.ingredients().size(); index++) {
                var button = new InlineKeyboardButton(ingredientList.ingredients().get(index));
                button.setCallbackData(StringUtils.appendNumberToCallbackData(String.format("%s-%s", CALLBACK_PREFIX, CALLBACK_PIZZA_INGREDIENT_TOGGLE), index));

                listOfIngredientButtons.add(button);
            }

            venueIngredientButtons.put(venue.getId(),
                    new IngredientButtonList(
                        inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCheckmark(listOfIngredientButtons, maxIngredientKeyboardColumns, maxIngredientKeyboardRows, CALLBACK_PREFIX),
                        Timestamp.from(Instant.now())));
        }
    }

    private String getMessageStringForIngredientToggle(CachedUser user) {
        var text = localizationService.getString("pizza.selectingredientswithlist");
        var ingredients = user.getSelectedUserIngredients();

        if (ingredients == null || ingredients.isEmpty())
            return localizationService.getString("pizza.selectingredients");

        StringBuilder ingredientString = new StringBuilder();

        for (int i = 0; i < ingredients.size(); i++) {
            ingredientString.append(pizzaService.resolveIngredientFromIndex(user.getSelectedVenue().getId(), ingredients.get(i)));
            if (i < ingredients.size() - 1) {
                ingredientString.append(", ");
            }
        }

        text = StringUtils.replacePropertiesVariable("ingredients", ingredientString.toString(), text);
        return text;
    }

    private void toggleSelectedIngredient(CachedUser user, int ingredientIndex) {
        user.toggleSelectedIngredient(ingredientIndex);
        cachedUserRepository.save(user);
    }

    private String formatPizzaForMessage(CachedUser user, Pizza pizza) {
        var pizzaInfoText = localizationService.getString("pizza.info");
        var venue = user.getSelectedVenue();

        var pizzaPrice = pizza.getPrice();
        var additionalInfo = "";

        if (user.isGlutenIntolerant() && venue.supportsGlutenFree() && user.isLactoseIntolerant() && venue.supportsLactoseFree()) {
            additionalInfo = "\n" + localizationService.getString("pizza.additionalinfo.glutenandlactosefree");
        } else {
            if (user.isGlutenIntolerant() && venue.supportsGlutenFree()) {
                additionalInfo = "\n" + localizationService.getString("pizza.additionalinfo.glutenfree");
            }

            if (user.isLactoseIntolerant() && venue.supportsLactoseFree()) {
                additionalInfo = "\n" + localizationService.getString("pizza.additionalinfo.lactosefree");
            }
        }

        var price = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(pizzaPrice);

        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_number",
                pizza.getMenuNumber(), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_name",
                pizza.getName(), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_price",
                StringUtils.escapeForMarkdownV2Format(price), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("diet_compatibility",
                StringUtils.escapeForMarkdownV2Format(getPizzaDietString(pizza.getMinimumUserDiet())), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_ingredients", StringUtils.escapeForMarkdownV2Format(formatIngredientListAsReadableString(pizza.getIngredients())), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("additional_info", StringUtils.escapeForMarkdownV2Format(additionalInfo), pizzaInfoText);

        return pizzaInfoText;
    }

    private String formatIngredientListAsReadableString(List<String> ingredients) {
        var ingredientsWithGrammar = new ArrayList<String>();
        for (var ingredient: ingredients) {
            ingredientsWithGrammar.add(StringUtils.applyGrammarRules(ingredient, botProperties.getPrimaryLocale()));
        }

        ingredients = ingredientsWithGrammar;

        if (ingredients.size() == 1)
            return ingredients.get(0);

        var andString = localizationService.getString("pizza.ingredients.and");

        StringBuilder ingredientsString = new StringBuilder();

        if (ingredients.size() == 1)
            return ingredients.get(0);

        for (int index = 0; index < ingredients.size(); index++) {
            if (ingredients.size() > 2 && index < ingredients.size() - 2) {
                ingredientsString
                        .append(ingredients.get(index))
                        .append(", ");
            }

            if (index == ingredients.size() - 2) {
                ingredientsString
                        .append(ingredients.get(index))
                        .append(" ")
                        .append(andString)
                        .append(" ");
            }

            if (index == ingredients.size() - 1) {
                ingredientsString.append(ingredients.get(index));
            }

        }

        return ingredientsString.toString();
    }

    private String getPizzaDietString(UserDiet diet) {
        return localizationService.getString(String.format("pizza.diet.%s", diet.toString().toLowerCase()));
    }
}
