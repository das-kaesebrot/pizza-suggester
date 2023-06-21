package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.exceptions.NotAuthorizedException;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import eu.kaesebrot.dev.pizzabot.properties.TelegramBotProperties;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.repository.PizzaRepository;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import eu.kaesebrot.dev.pizzabot.service.InlineKeyboardService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;

@Service
public class VenueEditSubMenuServiceImpl implements VenueEditSubMenuService {
    Logger logger = LoggerFactory.getLogger(VenueEditSubMenuServiceImpl.class);
    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final PizzaRepository pizzaRepository;
    private final InlineKeyboardService inlineKeyboardService;
    private final LocalizationService localizationService;
    private final TelegramBotProperties botProperties;
    private List<List<List<InlineKeyboardButton>>> pagedVenueSelectionMenu;
    private Timestamp lastPagedVenueSelectionUpdate;
    private long lastAmountOfVenuesInRepository = 0;

    HashMap<Long, Long> venuesBeingEditedByUsers = new HashMap<>();
    HashMap<Long, Map.Entry<Long, Integer>> activeEditMenus = new HashMap<>();

    public VenueEditSubMenuServiceImpl(CachedUserRepository cachedUserRepository, VenueRepository venueRepository, PizzaRepository pizzaRepository, InlineKeyboardService inlineKeyboardService, LocalizationService localizationService, TelegramBotProperties botProperties) {
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.pizzaRepository = pizzaRepository;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localizationService = localizationService;
        this.botProperties = botProperties;
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {

        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        var sanitizedData = stripCallbackPrefix(query.getData());
        int number = StringUtils.getNumberFromCallbackData(sanitizedData);
        sanitizedData = StringUtils.stripNumberFromCallbackData(sanitizedData);

        switch (sanitizedData) {
            case CALLBACK_ROOT_MENU:
            case CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX + "-" + InlineKeyboardService.CALLBACK_NAVIGATION_BACK:
                handleButtonPressGetRootMenu(user, query.getMessage().getMessageId(), bot);
                activeEditMenus.remove(user.getChatId());
                break;

            case CALLBACK_CREATE_VENUE:
                bot.execute(handleButtonPressCreateVenue(user));
                break;

            case CALLBACK_SUBMENU_PREFIX:
                handleButtonPressEditInSubmenuEditVenueForSpecificVenue(user, query.getMessage().getMessageId(), number, bot);
                break;

            // used for scrolling through venues in edit selection menu
            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
                if (pagedVenueSelectionMenu.size() > 1) {
                    var editVenueMenuMessage = new EditMessageReplyMarkup();
                    editVenueMenuMessage.setChatId(user.getChatId().toString());
                    editVenueMenuMessage.setMessageId(query.getMessage().getMessageId());
                    editVenueMenuMessage.setReplyMarkup(getVenueSelectionMarkup(number));

                    bot.execute(editVenueMenuMessage);
                }
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
                reply.setText(localizationService.getString("admin.pagepress"));
                break;

            case CALLBACK_EDIT_NAME:
            case CALLBACK_EDIT_URL:
            case CALLBACK_EDIT_PHONE:
            case CALLBACK_EDIT_ADDRESS:
            case CALLBACK_EDIT_COORDINATES:
            case CALLBACK_EDIT_PIZZAS:
                bot.execute(handleButtonPressChangeVenue(user, sanitizedData, number));
                break;

            case CALLBACK_EDIT_GLUTENFREE:
            case CALLBACK_EDIT_LACTOSEFREE:
                bot.execute(handleButtonPressChangeVenue(user, sanitizedData, number));
                tryUpdateEditMenuAfterVenueModification(user, bot);
                break;

            case CALLBACK_DELETE:
                bot.execute(handleButtonPressDeleteVenue(user, number));
                activeEditMenus.remove(user.getChatId());
                break;

            default:
                throw new IllegalArgumentException(String.format("Unknown callback data for admin venue edit sub menu! Given data: %s, CallbackQuery: %s", query.getData(), query));
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {

        switch (StringUtils.stripNumberFromCallbackData(stripCallbackPrefix(query.getData()))) {
            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
            case InlineKeyboardService.CALLBACK_NAVIGATION_BACK:
            case CALLBACK_SUBMENU_PREFIX:
            case CALLBACK_CREATE_VENUE:
            case CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX + "-" + InlineKeyboardService.CALLBACK_NAVIGATION_BACK:
            case CALLBACK_ROOT_MENU:
            case CALLBACK_EDIT_NAME:
            case CALLBACK_EDIT_URL:
            case CALLBACK_EDIT_PHONE:
            case CALLBACK_EDIT_ADDRESS:
            case CALLBACK_EDIT_COORDINATES:
            case CALLBACK_EDIT_PIZZAS:
            case CALLBACK_EDIT_GLUTENFREE:
            case CALLBACK_EDIT_LACTOSEFREE:
                return false;

            default:
                return true;
        }
    }

    @Override
    public String getCallbackPrefix() {
        return String.format("%s-%s", AdminMenuService.CALLBACK_PREFIX, CALLBACK_PREFIX);
    }
    @Override
    public BotApiMethod<?> handleCsvUpload(CachedUser user, String fileId, PizzaSuggesterBot bot) throws TelegramApiException, IOException {

        if (!user.hasState(UserState.MODIFYING_VENUE))
            throw new RuntimeException(String.format("User doesn't have required state %s", UserState.MODIFYING_VENUE));

        // throw if we're not expecting the user to send us a CSV file
        if (!user.hasState(UserState.SENDING_VENUE_CSV))
            throw new RuntimeException(String.format("User doesn't have required state %s", UserState.SENDING_VENUE_CSV));

        if (!user.isAdmin())
            throw new NotAuthorizedException(String.format("User %s is not an admin!", user.getChatId()));

        var venue = venueRepository.findById(venuesBeingEditedByUsers.get(user.getChatId())).get();

        var reply = new SendMessage();
        reply.setChatId(user.getChatId());
        reply.setText(localizationService.getString("admin.csvuploadsuccess"));

        List<List<String>> rows = new java.util.ArrayList<>(List.of());

        logger.info(String.format("Reading new pizza CSV for venue %s", venue));

        logger.debug(String.format("Handling new document by %s: %s", user , fileId));
        String filePath = bot.execute(new GetFile(fileId)).getFileUrl(botProperties.getBotToken());

        var outputStream = new ByteArrayOutputStream();

        try {
            logger.debug(String.format("Downloading CSV file from %s", filePath));
            InputStream in = new URL(filePath).openStream();
            IOUtils.copy(in, outputStream);

            String csvContent = outputStream.toString(StandardCharsets.UTF_8);

            try (BufferedReader br = new BufferedReader(new StringReader(csvContent))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(";");

                    rows.add(Arrays.asList(values));
                }
            }

        } catch (IOException e) {
            logger.error(String.format("Encountered an exception while handling file from path '%s'", filePath), e);
            throw e;
        }

        var pizzas = new ArrayList<Pizza>();
        int errorCounter = 0;

        for (var row: rows) {
            try {
                // Assuming the following data structure:
                // Pizza Number;Pizza Name;Price;Ingredients (comma-separated);Diet
                // 2;Pizza;5.5;Tomatensoße,Käse,Salami;vegeterian
                // 2;Pizza;5.5;Tomatensoße,Käse,Salami;2

                logger.debug(String.format("Parsing row to pizza object: %s", row));
                String number = row.get(0).trim();
                String name = row.get(1).trim();
                BigDecimal price = new BigDecimal(row.get(2));
                List<String> ingredients = List.of(row.get(3).trim().split(","));
                UserDiet minimumUserDiet;
                try {
                    minimumUserDiet = UserDiet.valueOf(row.get(4).trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // try with ordinal instead of string
                    minimumUserDiet = UserDiet.values()[Integer.parseInt(row.get(4).trim())];
                }

                Pizza pizza = new Pizza(number, name, price, ingredients, minimumUserDiet, venue);
                pizzas.add(pizza);

                logger.debug(String.format("Created new pizza: %s", pizza));

            } catch (Exception e) {
                logger.error("Encountered an exception while parsing row from pizza CSV. Skipping row.", e);
                errorCounter++;
            }
        }

        user.removeState(UserState.MODIFYING_VENUE);
        user.removeState(UserState.SENDING_VENUE_CSV);

        // remove all previous pizzas for that venue
        if (pizzaRepository.existsPizzasByVenue(venue))
            pizzaRepository.deletePizzasByVenue(venue);

        pizzaRepository.saveAllAndFlush(pizzas);
        cachedUserRepository.save(user);

        logger.info(String.format("Processed %d rows and generated %d pizza objects, encountered %d errors", rows.size(), pizzas.size(), errorCounter));

        tryUpdateEditMenuAfterVenueModification(user, bot);

        return reply;
    }

    @Override
    public SendMessage handleVenueCreationReply(CachedUser user, String message) {
        if (!user.isAdmin())
            throw new NotAuthorizedException(String.format("User %d is not allowed to create new venues!", user.getChatId()));

        if (!user.hasState(UserState.SENDING_VENUE_NAME))
            throw new RuntimeException(String.format("User is not in required state %s!", UserState.SENDING_VENUE_NAME));

        var venue = new Venue();
        venue.setName(message.trim());

        user.removeState(UserState.CREATING_VENUE);
        user.removeState(UserState.SENDING_VENUE_NAME);

        venueRepository.saveAndFlush(venue);
        cachedUserRepository.saveAndFlush(user);

        var text = localizationService.getString("admin.venues.create.success");
        text = StringUtils.replacePropertiesVariable("name", venue.getName(), text);
        text = StringUtils.replacePropertiesVariable("venue_edit_button_label", localizationService.getString("admin.venues.edit"), text);

        return new SendMessage(user.getChatId().toString(), text);
    }

    @Override
    public SendMessage handleVenueModificationReply(CachedUser user, String message, PizzaSuggesterBot bot) {
        if (!user.isAdmin())
            throw new NotAuthorizedException(String.format("User %d is not allowed to modify venues!", user.getChatId()));

        if (!user.hasState(UserState.MODIFYING_VENUE))
            throw new RuntimeException(String.format("User is not in required state %s!", UserState.MODIFYING_VENUE));

        var venue = venueRepository.findById(venuesBeingEditedByUsers.get(user.getChatId())).get();

        try {
            user.removeState(UserState.MODIFYING_VENUE);

            var trimmedText = message.trim();

            if (user.hasState(UserState.SENDING_VENUE_NAME)) {
                user.removeState(UserState.SENDING_VENUE_NAME);

                venue.setName(trimmedText);

            } else if (user.hasState(UserState.SENDING_VENUE_ADDRESS)) {
                user.removeState(UserState.SENDING_VENUE_ADDRESS);

                var venueInfo = venue.getVenueInfo();
                venueInfo.setAddress(trimmedText);
                venue.setVenueInfo(venueInfo);

            } else if (user.hasState(UserState.SENDING_VENUE_COORDINATES)) {
                user.removeState(UserState.SENDING_VENUE_COORDINATES);

                var venueInfo = venue.getVenueInfo();
                venueInfo.setCoordinatesByString(trimmedText);
                venue.setVenueInfo(venueInfo);

            } else if (user.hasState(UserState.SENDING_VENUE_PHONE_NUMBER)) {
                user.removeState(UserState.SENDING_VENUE_PHONE_NUMBER);

                var venueInfo = venue.getVenueInfo();
                venueInfo.setPhoneNumber(trimmedText);
                venue.setVenueInfo(venueInfo);
            } else if (user.hasState(UserState.SENDING_VENUE_URL)) {
                user.removeState(UserState.SENDING_VENUE_URL);

                var venueInfo = venue.getVenueInfo();
                venueInfo.setUrl(trimmedText);
                venue.setVenueInfo(venueInfo);
            } else if (user.hasState(UserState.SENDING_GLUTENFREE_MARKUP)) {
                user.removeState(UserState.SENDING_GLUTENFREE_MARKUP);

                venue.setGlutenFreeMarkup(BigDecimal.valueOf(Double.parseDouble(trimmedText)));
            } else if (user.hasState(UserState.SENDING_LACTOSEFREE_MARKUP)) {
                user.removeState(UserState.SENDING_LACTOSEFREE_MARKUP);

                venue.setLactoseFreeMarkup(BigDecimal.valueOf(Double.parseDouble(trimmedText)));
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } finally {
            // always save, no matter what
            venueRepository.saveAndFlush(venue);
            cachedUserRepository.saveAndFlush(user);

            venuesBeingEditedByUsers.remove(user.getChatId());
        }

        var text = localizationService.getString("admin.venues.edit.success");
        text = StringUtils.replacePropertiesVariable("name", venue.getName(), text);

        tryUpdateEditMenuAfterVenueModification(user, bot);

        return new SendMessage(user.getChatId().toString(), text);
    }

    private void tryUpdateEditMenuAfterVenueModification(CachedUser user, PizzaSuggesterBot bot) {
        var entry = activeEditMenus.get(user.getChatId());

        if (entry == null)
            return;

        try {
            handleButtonPressEditInSubmenuEditVenueForSpecificVenue(user, entry.getValue(), Math.toIntExact(entry.getKey()), bot);
        } catch (Exception e) {
            logger.error("Encountered exception while updating edit menu. Handling gracefully.", e);
        }
    }

    private SendMessage handleButtonPressCreateVenue(CachedUser user) {
        user.addState(UserState.CREATING_VENUE);
        user.addState(UserState.SENDING_VENUE_NAME);

        cachedUserRepository.save(user);

        return new SendMessage(user.getChatId().toString(), localizationService.getString("admin.venues.create.name"));
    }

    private SendMessage handleButtonPressDeleteVenue(CachedUser user, int venueId) {

        if (!user.isAdmin())
            throw new NotAuthorizedException(String.format("User %d is not allowed to modify venues!", user.getChatId()));

        var venue = venueRepository.findById((long) venueId).get();
        var name = venue.getName();

        pizzaRepository.deleteAll(pizzaRepository.findByVenue(venue));
        venueRepository.deleteById(venue.getId());

        var text = localizationService.getString("admin.venues.edit.delete.reply");
        text = StringUtils.replacePropertiesVariable("venue_name", name, text);

        return new SendMessage(user.getChatId().toString(), text);
    }

    private SendMessage handleButtonPressChangeVenue(CachedUser user, String operation, int venueId) {
        var message = new SendMessage();
        user.addState(UserState.MODIFYING_VENUE);
        message.setChatId(user.getChatId());

        var venue = venueRepository.findById((long) venueId).get();
        boolean addToEditMap = true;

        switch (operation) {
            case CALLBACK_EDIT_NAME:
                user.addState(UserState.SENDING_VENUE_NAME);
                message.setText(localizationService.getString("admin.venues.edit.name.reply"));
                break;

            case CALLBACK_EDIT_URL:
                user.addState(UserState.SENDING_VENUE_URL);
                message.setText(localizationService.getString("admin.venues.edit.url.reply"));
                break;

            case CALLBACK_EDIT_PHONE:
                user.addState(UserState.SENDING_VENUE_PHONE_NUMBER);
                message.setText(localizationService.getString("admin.venues.edit.number.reply"));
                break;

            case CALLBACK_EDIT_ADDRESS:
                user.addState(UserState.SENDING_VENUE_ADDRESS);
                message.setText(localizationService.getString("admin.venues.edit.address.reply"));
                break;

            case CALLBACK_EDIT_COORDINATES:
                user.addState(UserState.SENDING_VENUE_COORDINATES);
                message.setText(localizationService.getString("admin.venues.edit.coordinates.reply"));
                break;

            case CALLBACK_EDIT_PIZZAS:
                user.addState(UserState.SENDING_VENUE_CSV);
                message.setText(localizationService.getString("admin.venues.edit.pizzas.reply"));
                break;

            case CALLBACK_EDIT_GLUTENFREE:
                if (venue.supportsGlutenFree()) {
                    venue.disableGlutenFreeSupport();
                    message.setText(localizationService.getString("admin.venues.edit.glutenfree.off"));
                    venueRepository.save(venue);
                    addToEditMap = false;

                } else {
                    user.addState(UserState.SENDING_GLUTENFREE_MARKUP);
                    message.setText(localizationService.getString("admin.venues.edit.glutenfree.reply"));
                }
                break;

            case CALLBACK_EDIT_LACTOSEFREE:
                if (venue.supportsLactoseFree()) {
                    venue.disableLactoseFreeSupport();
                    message.setText(localizationService.getString("admin.venues.edit.lactosefree.off"));
                    venueRepository.save(venue);
                    addToEditMap = false;

                } else {
                    user.addState(UserState.SENDING_LACTOSEFREE_MARKUP);
                    message.setText(localizationService.getString("admin.venues.edit.lactosefree.reply"));
                }
                break;
        }

        if (addToEditMap)
            venuesBeingEditedByUsers.put(user.getChatId(), (long) venueId);

        cachedUserRepository.saveAndFlush(user);

        return message;
    }


    private void handleButtonPressEditInSubmenuEditVenueForSpecificVenue(CachedUser user, int messageId, int venueId, PizzaSuggesterBot bot) throws TelegramApiException {
        var editMarkup = new EditMessageReplyMarkup();
        var venue = venueRepository.findById((long) venueId).get();

        activeEditMenus.put(user.getChatId(), Map.entry(venue.getId(), messageId));

        editMarkup.setChatId(user.getChatId().toString());
        editMarkup.setMessageId(messageId);
        editMarkup.setReplyMarkup(getVenueSpecificOpsMarkup(venueId));

        bot.execute(getVenueEditMessageText(user, messageId, venueId));
        bot.execute(editMarkup);
    }

    private EditMessageText getVenueEditMessageText(CachedUser user, int messageId, int venueId) {
        var editMessageText = new EditMessageText();
        var venue = venueRepository.findById((long) venueId).get();

        var text = localizationService.getString("admin.venues.edit.venue");

        var glutenFree = localizationService.getString("admin.venues.edit.venue.glutenfree.false");
        var lactoseFree = localizationService.getString("admin.venues.edit.venue.lactosefree.false");

        var currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);

        var url = venue.getVenueInfo().getUrl();
        var urlString = "";
        if (url != null)
            urlString = url.toString();

        if (venue.supportsGlutenFree()) {
            glutenFree = localizationService.getString("admin.venues.edit.venue.glutenfree.true");
            glutenFree = StringUtils.replacePropertiesVariable("markup", StringUtils.escapeForMarkdownV2Format(currencyFormat.format(venue.getGlutenFreeMarkup())), glutenFree);
        }


        if (venue.supportsLactoseFree()) {
            lactoseFree = localizationService.getString("admin.venues.edit.venue.lactosefree.true");
            lactoseFree = StringUtils.replacePropertiesVariable("markup", StringUtils.escapeForMarkdownV2Format(currencyFormat.format(venue.getLactoseFreeMarkup())), lactoseFree);
        }

        text = StringUtils.replacePropertiesVariable("venue_name", StringUtils.escapeForMarkdownV2Format(venue.getName()), text);
        text = StringUtils.replacePropertiesVariable("venue_id", venue.getId().toString(), text);
        text = StringUtils.replacePropertiesVariable("venue_url", StringUtils.escapeForMarkdownV2Format(urlString), text);
        text = StringUtils.replacePropertiesVariable("venue_address", StringUtils.escapeForMarkdownV2Format(venue.getVenueInfo().getAddress()), text);
        text = StringUtils.replacePropertiesVariable("venue_coordinates", StringUtils.escapeForMarkdownV2Format(venue.getVenueInfo().getCoordinatesString()), text);
        text = StringUtils.replacePropertiesVariable("venue_number", StringUtils.escapeForMarkdownV2Format(venue.getVenueInfo().getPhoneNumber()), text);
        text = StringUtils.replacePropertiesVariable("venue_pizza_amount", String.valueOf(venue.getPizzaMenu().size()), text);
        text = StringUtils.replacePropertiesVariable("venue_gluten_free", glutenFree, text);
        text = StringUtils.replacePropertiesVariable("venue_lactose_free", lactoseFree, text);

        editMessageText.setChatId(user.getChatId().toString());
        editMessageText.setMessageId(messageId);
        editMessageText.setParseMode(ParseMode.MARKDOWNV2);
        editMessageText.setText(text);

        return editMessageText;
    }


    private void handleButtonPressGetRootMenu(CachedUser user, int messageId, PizzaSuggesterBot bot) throws TelegramApiException {
        if (venueRepository.findAll().isEmpty()) {
            return;
        }

        var message = new EditMessageText();
        message.setChatId(user.getChatId());
        message.setMessageId(messageId);
        message.setText(localizationService.getString("admin.venues.edit.message"));

        var editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(user.getChatId());
        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setReplyMarkup(getVenueSelectionMarkup(0));

        bot.execute(message);
        bot.execute(editMessageReplyMarkup);
    }

    private InlineKeyboardMarkup getVenueSpecificOpsMarkup(int venueId) {
        var venue = venueRepository.findById((long) venueId).get();

        var keyboard = new InlineKeyboardMarkup();

        var pagedMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithBackButtonWithoutPageButtons(getVenueSpecificOps(venue), EDIT_MENU_COLUMNS, EDIT_MENU_ROWS, getCallbackPrefix() + "-" + CALLBACK_SUBMENU_VENUE_SPECIFIC_PREFIX);

        keyboard.setKeyboard(pagedMenu.get(0));

        return keyboard;
    }

    private List<InlineKeyboardButton> getVenueSpecificOps(Venue venue) {
        var buttonChangeName = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.name"));
        buttonChangeName.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_EDIT_NAME), Math.toIntExact(venue.getId())));

        var buttonChangeUrl = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.url"));
        buttonChangeUrl.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_EDIT_URL), Math.toIntExact(venue.getId())));

        var buttonChangeAddress = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.address"));
        buttonChangeAddress.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_EDIT_ADDRESS), Math.toIntExact(venue.getId())));

        var buttonChangeCoordinates = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.coordinates"));
        buttonChangeCoordinates.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_EDIT_COORDINATES), Math.toIntExact(venue.getId())));

        var buttonChangeNumber = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.number"));
        buttonChangeNumber.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_EDIT_PHONE), Math.toIntExact(venue.getId())));

        var buttonUploadPizzaCsv = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.pizzas"));
        buttonUploadPizzaCsv.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_EDIT_PIZZAS), Math.toIntExact(venue.getId())));

        var buttonToggleGlutenFree = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.glutenfree"));
        buttonToggleGlutenFree.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_EDIT_GLUTENFREE), Math.toIntExact(venue.getId())));

        var buttonToggleLactoseFree = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.lactosefree"));
        buttonToggleLactoseFree.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_EDIT_LACTOSEFREE), Math.toIntExact(venue.getId())));

        var buttonDelete = new InlineKeyboardButton(localizationService.getString("admin.venues.edit.delete"));
        buttonDelete.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_DELETE), Math.toIntExact(venue.getId())));

        return List.of(buttonChangeName, buttonChangeUrl, buttonChangeAddress, buttonChangeCoordinates, buttonChangeNumber, buttonUploadPizzaCsv, buttonToggleGlutenFree, buttonToggleLactoseFree, buttonDelete);
    }

    private List<InlineKeyboardButton> getVenueButtons() {
        var venueButtons = new ArrayList<InlineKeyboardButton>();

        for (Venue venue : venueRepository.findAll())
        {
            var button = new InlineKeyboardButton(formatVenueForButton(venue));
            button.setCallbackData(StringUtils.appendNumberToCallbackData(prependCallbackPrefix(CALLBACK_SUBMENU_PREFIX), Math.toIntExact(venue.getId())));
            venueButtons.add(button);
        }

        return venueButtons;
    }

    private String formatVenueForButton(Venue venue) {
        var venueInfoText = localizationService.getString("venue.info");
        venueInfoText = StringUtils.replacePropertiesVariable("venue_name", venue.getName(), venueInfoText);
        venueInfoText = StringUtils.replacePropertiesVariable("venue_address", venue.getVenueInfo().getAddress(), venueInfoText);

        return venueInfoText;
    }


    private InlineKeyboardMarkup getVenueSelectionMarkup(int page) {
        regenerateMenuCaches();

        var keyboard = new InlineKeyboardMarkup();

        keyboard.setKeyboard(pagedVenueSelectionMenu.get(page));

        return keyboard;
    }


    private void regenerateMenuCaches() {
        if (!venueRepository.findAll().isEmpty() && (pagedVenueSelectionMenu == null ||
                pagedVenueSelectionMenu.isEmpty() ||
                lastPagedVenueSelectionUpdate == null ||
                venueRepository.existsByModifiedAtAfter(lastPagedVenueSelectionUpdate) ||
                venueRepository.existsByVenueInfoModifiedAtAfter(lastPagedVenueSelectionUpdate) ||
                lastAmountOfVenuesInRepository != venueRepository.count()
        )) {
            pagedVenueSelectionMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithBackButton(getVenueButtons(), MENU_COLUMNS, MENU_ROWS, getCallbackPrefix());
            lastPagedVenueSelectionUpdate = Timestamp.from(Instant.now());
            lastAmountOfVenuesInRepository = venueRepository.count();
        }
    }
}
