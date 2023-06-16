package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.exceptions.PendingVenueSelectionException;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import eu.kaesebrot.dev.pizzabot.properties.VersionProperties;
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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class UserMenuServiceImpl implements UserMenuService {
    private static final String CALLBACK_DIET_PREFIX = "diet";
    private static final String CALLBACK_VENUE_PREFIX = "venue";
    private static final String CALLBACK_VENUE_SELECTION = CALLBACK_VENUE_PREFIX + "-select";
    private static final String MESSAGES_LABEL_DIET_PREFIX = "label.diet";
    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final PizzaService pizzaService;
    private final IngredientMenuService ingredientMenuService;
    private final InlineKeyboardService inlineKeyboardService;
    private final LocalizationService localizationService;
    private final VersionProperties versionProperties;
    private List<List<List<InlineKeyboardButton>>> pagedVenueSelectionMenu;
    private Timestamp lastPagedVenueSelectionUpdate;

    public UserMenuServiceImpl(CachedUserRepository cachedUserRepository, VenueRepository venueRepository, PizzaService pizzaService, IngredientMenuService ingredientMenuService, InlineKeyboardService inlineKeyboardService, LocalizationService localizationService) {
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.pizzaService = pizzaService;
        this.ingredientMenuService = ingredientMenuService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localizationService = localizationService;

        versionProperties = new VersionProperties();
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException
    {
        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        var sanitizedData = stripCallbackPrefix(query.getData());

        if (sanitizedData.startsWith(CALLBACK_VENUE_PREFIX))
        {
            handleVenueCallback(user, sanitizedData, query, bot);
        }
        else if (sanitizedData.startsWith(CALLBACK_DIET_PREFIX))
        {
            var selectedDiet = sanitizedData.replace(String.format("%s-", CALLBACK_DIET_PREFIX), "");

            user.setUserDiet(UserDiet.valueOf(selectedDiet.toUpperCase()));
            user.removeState(UserState.SELECTING_DIET);
            cachedUserRepository.saveAndFlush(user);

            bot.execute(new SendMessage(query.getFrom().getId().toString(),
                    String.format("%s: %s", localizationService.getString("select.dietsuccess"),
                            localizationService.getString(String.format("%s.%s", MESSAGES_LABEL_DIET_PREFIX, selectedDiet.toLowerCase()))
                    ))
            );
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        return true;
    }

    @Override
    public SendMessage getDietSelection(CachedUser user) {
        SendMessage dietSelection = new SendMessage();
        dietSelection.setChatId(user.getChatId());
        dietSelection.setText(localizationService.getString("select.diet"));
        dietSelection.setReplyMarkup(getDietSelectionMarkup());

        user.addState(UserState.SELECTING_DIET);
        cachedUserRepository.save(user);

        return dietSelection;
    }

    private void handleVenueCallback(CachedUser user, String sanitizedData, CallbackQuery query, PizzaSuggesterBot bot) {
        var selectedVenue = sanitizedData;
        selectedVenue = selectedVenue.replace(String.format("%s-", CALLBACK_VENUE_PREFIX), "");

        long venueId = Long.parseLong(selectedVenue);
        var venue = venueRepository.findById(venueId);

        if (venue.isEmpty())
            throw new RuntimeException("Couldn't find a venue by given id!");

        user.setSelectedVenue(venue.get());
        user.removeState(UserState.SELECTING_VENUE);
        cachedUserRepository.saveAndFlush(user);
    }

    private InlineKeyboardMarkup getDietSelectionMarkup() {
        var dietButtonRows = new ArrayList<List<InlineKeyboardButton>>();
        var keyboard = new InlineKeyboardMarkup();

        for (UserDiet diet : UserDiet.values())
        {
            var button = new InlineKeyboardButton(localizationService.getString(String.format("%s.%s", MESSAGES_LABEL_DIET_PREFIX, diet.name().toLowerCase())));
            button.setCallbackData(prependCallbackPrefix(String.format("%s-%s", CALLBACK_DIET_PREFIX, diet.name().toLowerCase())));
            dietButtonRows.add(List.of(button));
        }

        keyboard.setKeyboard(dietButtonRows);

        return keyboard;
    }

    @Override
    public SendMessage getVenueSelection(CachedUser user) {
        SendMessage venueSelection = new SendMessage();
        venueSelection.setChatId(user.getChatId());
        venueSelection.setText(localizationService.getString("select.venue"));
        venueSelection.setReplyMarkup(getVenueSelectionMarkup());

        user.addState(UserState.SELECTING_VENUE);
        cachedUserRepository.save(user);

        return venueSelection;
    }

    @Override
    public SendMessage getRandomPizza(CachedUser user) {
        if (user.getSelectedVenue() == null)
            throw new PendingVenueSelectionException("No venue selected by user yet!");

        var pizza = pizzaService.getRandomPizza(user.getSelectedVenue(), user);

        var text = formatPizzaForMessage(pizza);
        var formattedPizzaRandText = localizationService.getString("pizza.random");
        text = StringUtils.replacePropertiesVariable("pizza_info", text, formattedPizzaRandText);

        SendMessage reply = new SendMessage(user.getChatId().toString(), text);
        reply.setParseMode(ParseMode.MARKDOWNV2);

        return reply;
    }

    @Override
    public SendMessage getSetupMessages(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException {
        bot.execute(new SendMessage(user.getChatId().toString(), localizationService.getString("reply.firstrun")));

        if (!venueRepository.findAll().isEmpty()) {
            bot.execute(getVenueSelection(user));
        }

        bot.execute(getDietSelection(user));

        bot.execute(new SendMessage(user.getChatId().toString(), localizationService.getString("select.disclaimer")));

        return getHelpMessage(user);
    }

    @Override
    public SendMessage getHelpMessageWithGreeting(CachedUser user) {
        var msg = new SendMessage(user.getChatId().toString(),
                String.format("%s\n%s", localizationService.getString("reply.greeting"), localizationService.getString("reply.help")));
        msg.setParseMode(ParseMode.MARKDOWNV2);

        return msg;
    }

    @Override
    public SendMessage getHelpMessage(CachedUser user) {
        var msg = new SendMessage(user.getChatId().toString(), localizationService.getString("reply.help"));
        msg.setParseMode(ParseMode.MARKDOWNV2);

        return msg;
    }

    @Override
    public SendMessage getAboutMessage(CachedUser user) {
        var text = localizationService.getString("reply.about");
        text = StringUtils.replacePropertiesVariable("bot_handle", "KantineBot", text);
        text = StringUtils.replacePropertiesVariable("technical_name", "pizza-suggester", text);
        text = StringUtils.replacePropertiesVariable("git_hash", versionProperties.getGitHash(), text);
        text = StringUtils.replacePropertiesVariable("git_branch", versionProperties.getGitBranch(), text);
        text = StringUtils.replacePropertiesVariable("build_date", versionProperties.getBuildDate().toString(), text);

        var msg = new SendMessage(user.getChatId().toString(), text);
        msg.setParseMode(ParseMode.MARKDOWNV2);
        msg.disableWebPagePreview();

        return msg;
    }

    @Override
    public SendMessage getIngredientSelectionMenu(CachedUser user) {
        if (user.getSelectedVenue() == null)
            throw new PendingVenueSelectionException("No venue selected by user yet!");

        SendMessage reply = new SendMessage(user.getChatId().toString(), localizationService.getString("pizza.random"));
        reply.setParseMode(ParseMode.MARKDOWNV2);

        reply.setReplyMarkup(ingredientMenuService.getInitialKeyboard(user.getSelectedVenue().getId()));

        return reply;
    }

    private InlineKeyboardMarkup getVenueSelectionMarkup() {
        var keyboard = new InlineKeyboardMarkup();
        regenerateMenuCaches();

        keyboard.setKeyboard(pagedVenueSelectionMenu.get(0));

        return keyboard;
    }

    private String stripCallbackPrefix(String data) {
        return StringUtils.stripCallbackPrefix(CALLBACK_PREFIX, data);
    }

    private String prependCallbackPrefix(String data) {
        return StringUtils.prependCallbackPrefix(CALLBACK_PREFIX, data);
    }

    private String formatPizzaForMessage(Pizza pizza) {
        var pizzaInfoText = localizationService.getString("pizza.info");

        var pizzaDiet = localizationService.getString(String.format("%s.%s", MESSAGES_LABEL_DIET_PREFIX, pizza.getMinimumUserDiet().toString().toLowerCase()));

        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_number",
                pizza.getMenuNumber(), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_name",
                "Pizza", pizzaInfoText); // todo pizza name
        pizzaInfoText = StringUtils.replacePropertiesVariable("pizza_price",
                StringUtils.escapeForMarkdownV2Format(NumberFormat.getInstance(Locale.GERMANY).format(pizza.getPrice())), pizzaInfoText);
        pizzaInfoText = StringUtils.replacePropertiesVariable("diet_compatibility",
                pizzaDiet, pizzaInfoText);

        return pizzaInfoText;
    }


    private String formatVenueForButton(Venue venue) {
        // TODO venue formatting
        var pizzaInfoText = localizationService.getString("pizza.info");

        return pizzaInfoText;
    }

    private List<InlineKeyboardButton> getVenueButtons() {
        var venueButtons = new ArrayList<InlineKeyboardButton>();

        for (Venue venue : venueRepository.findAll())
        {
            var button = new InlineKeyboardButton(formatVenueForButton(venue));
            button.setCallbackData(prependCallbackPrefix(String.format("%s-%d", CALLBACK_VENUE_SELECTION, venue.getId())));
            venueButtons.add(button);
        }

        return venueButtons;
    }

    private void regenerateMenuCaches() {
        if (pagedVenueSelectionMenu == null ||
                pagedVenueSelectionMenu.isEmpty() ||
                lastPagedVenueSelectionUpdate == null ||
                !venueRepository.findByModifiedAtGreaterThan(lastPagedVenueSelectionUpdate).isEmpty()) {
            pagedVenueSelectionMenu = inlineKeyboardService.getPagedInlineKeyboardButtons(getVenueButtons(), VENUE_SELECTION_COLUMNS, VENUE_SELECTION_ROWS, true, CALLBACK_VENUE_PREFIX, false, false);
            lastPagedVenueSelectionUpdate = Timestamp.from(Instant.now());
        }
    }
}
