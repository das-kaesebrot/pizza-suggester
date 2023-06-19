package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.classes.IngredientButtonList;
import eu.kaesebrot.dev.pizzabot.exceptions.PendingVenueSelectionException;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
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
    private final VenueRepository venueRepository;
    private final InlineKeyboardService inlineKeyboardService;
    private final PizzaService pizzaService;
    private final LocalizationService localizationService;
    private HashMap<Long, IngredientButtonList> venueIngredientButtons = new HashMap<>();
    private HashMap<Long, List<Integer>> selectedUserIngredients = new HashMap<>();

    public PizzaMenuServiceImpl(VenueRepository venueRepository, LocalizationService localizationService, InlineKeyboardService inlineKeyboardService, PizzaService pizzaService, LocalizationService localizationService1) {
        this.venueRepository = venueRepository;
        this.inlineKeyboardService = inlineKeyboardService;
        this.pizzaService = pizzaService;
        this.localizationService = localizationService1;
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

        switch (sanitizedDataWithoutNumbers) {
            case CALLBACK_PIZZA_INGREDIENT_TOGGLE:
                toggleSelectedIngredient(user, getIngredientIndexFromCallbackData(sanitizedData));

                var editMessageText = new EditMessageText();
                editMessageText.setText(getMessageStringForIngredientToggle(user));
                editMessageText.setChatId(user.getChatId().toString());
                editMessageText.setMessageId(query.getMessage().getMessageId());

                bot.execute(editMessageText);
                break;


            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
                reply.setText(localizationService.getString("admin.pagepress"));
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
                var editMessageMarkup = new EditMessageReplyMarkup();
                editMessageMarkup.setChatId(user.getChatId().toString());
                editMessageMarkup.setMessageId(query.getMessage().getMessageId());
                editMessageMarkup.setReplyMarkup(getKeyboardForPage(user.getSelectedVenue(), StringUtils.getNumberFromCallbackData(sanitizedData)));

                bot.execute(editMessageMarkup);
                break;

            default:
                throw new IllegalArgumentException(String.format("Unknown callback data for pizza menu! Given data: %s, CallbackQuery: %s", query.getData(), query));
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        switch (StringUtils.stripNumberFromCallbackData(stripCallbackPrefix(query.getData()))) {
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

        var text = formatPizzaForMessage(pizza);
        var formattedPizzaRandText = localizationService.getString("pizza.random");
        text = StringUtils.replacePropertiesVariable("pizza_info", text, formattedPizzaRandText);
        text = StringUtils.replacePropertiesVariable("greeting", localizationService.getString("pizza.greeting"), text);

        SendMessage reply = new SendMessage(user.getChatId().toString(), text);
        reply.setParseMode(ParseMode.MARKDOWNV2);

        return reply;
    }

    @Override
    public SendMessage getIngredientSelectionMenu(CachedUser user) {
        if (user.getSelectedVenue() == null)
            throw new PendingVenueSelectionException("No venue selected by user yet!");

        // initialize ingredient selection array
        selectedUserIngredients.put(user.getChatId(), new ArrayList<>());

        SendMessage reply = new SendMessage(user.getChatId().toString(), localizationService.getString("pizza.random"));
        reply.setParseMode(ParseMode.MARKDOWNV2);

        reply.setReplyMarkup(getKeyboardForPage(user.getSelectedVenue(), 0));

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
        var ingredients = selectedUserIngredients.get(user.getChatId());

        if (ingredients == null || ingredients.isEmpty())
            return localizationService.getString("pizza.selectingredients");

        var ingredientString = "";

        for (int i = 0; i < ingredients.size(); i++) {
            ingredientString += pizzaService.resolveIngredientFromIndex(user.getSelectedVenue().getId(), ingredients.get(i));
            if (i < ingredients.size() - 1) {
                ingredientString += ", ";
            }
        }

        text = StringUtils.replacePropertiesVariable("ingredients", ingredientString, text);
        return text;
    }

    private void toggleSelectedIngredient(CachedUser user, int ingredientIndex) {
        var entry = selectedUserIngredients.get(user.getChatId());
        if (entry.contains(ingredientIndex))
            entry.remove(ingredientIndex);
        else
            entry.add(ingredientIndex);

        selectedUserIngredients.put(user.getChatId(), entry);
    }

    private int getIngredientIndexFromCallbackData(String data) {
        if (data.startsWith(CALLBACK_PIZZA_INGREDIENT_TOGGLE))
            return Integer.parseInt(data.replace(String.format("%s--", CALLBACK_PIZZA_INGREDIENT_TOGGLE), ""));

        return 0;
    }

    private String formatPizzaForMessage(Pizza pizza) {
        var pizzaInfoText = localizationService.getString("pizza.info");

        var pizzaDiet = localizationService.getString(String.format("%s.%s", UserMenuService.MESSAGES_LABEL_DIET_PREFIX, pizza.getMinimumUserDiet().toString().toLowerCase()));

        var price = NumberFormat.getCurrencyInstance(Locale.GERMANY).format(pizza.getPrice());

        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_number",
                pizza.getMenuNumber(), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_name",
                pizza.getName(), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_price",
                StringUtils.escapeForMarkdownV2Format(price), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("diet_compatibility",
                pizzaDiet, pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_ingredients", pizza.getIngredients().toString(), pizzaInfoText);

        return pizzaInfoText;
    }
}
