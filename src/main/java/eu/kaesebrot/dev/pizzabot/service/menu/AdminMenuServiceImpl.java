package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.exceptions.NotAuthorizedException;
import eu.kaesebrot.dev.pizzabot.model.AdminKey;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import eu.kaesebrot.dev.pizzabot.repository.AdminKeyRepository;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@Service
public class AdminMenuServiceImpl implements AdminMenuService {
    Logger logger = LoggerFactory.getLogger(AdminMenuServiceImpl.class);
    private final VenueRepository venueRepository;
    private final CachedUserRepository cachedUserRepository;
    private final AdminKeyRepository adminKeyRepository;
    private final UserMenuService userMenuService;
    private final InlineKeyboardService inlineKeyboardService;
    private final LocalizationService localizationService;

    public final String CALLBACK_ADMIN_VENUES_PREFIX = "v";
    // only used to get the submenu for editing venues from the admin menu
    public final String CALLBACK_ADMIN_VENUES_EDIT_MENU_OP = CALLBACK_ADMIN_VENUES_PREFIX + "-menu";
    // only used to initialize the venue creation process
    public final String CALLBACK_ADMIN_VENUES_CREATE = CALLBACK_ADMIN_VENUES_PREFIX + "-create";
    // used in venue selection menu before editing a venue (overview of all venues to edit)
    public final String CALLBACK_ADMIN_VENUES_EDIT_PREFIX = CALLBACK_ADMIN_VENUES_PREFIX + "-edit";

    // used for buttons when selecting an edit operation for a venue
    public final String CALLBACK_ADMIN_VENUES_EDIT_SUBMENU_PREFIX = CALLBACK_ADMIN_VENUES_EDIT_PREFIX + "-ops";
    // used for editing the name
    public final String CALLBACK_ADMIN_VENUES_EDIT_NAME = CALLBACK_ADMIN_VENUES_EDIT_SUBMENU_PREFIX + "-n";
    public final String CALLBACK_ADMIN_VENUES_EDIT_ADDRESS = CALLBACK_ADMIN_VENUES_EDIT_SUBMENU_PREFIX + "-a";
    public final String CALLBACK_ADMIN_VENUES_EDIT_URL = CALLBACK_ADMIN_VENUES_EDIT_SUBMENU_PREFIX + "-u";
    public final String CALLBACK_ADMIN_VENUES_EDIT_PHONE = CALLBACK_ADMIN_VENUES_EDIT_SUBMENU_PREFIX + "-p";
    public final String CALLBACK_ADMIN_VENUES_EDIT_PIZZAS = CALLBACK_ADMIN_VENUES_EDIT_SUBMENU_PREFIX + "-c";
    public final String CALLBACK_ADMIN_VENUES_DELETE = CALLBACK_ADMIN_VENUES_EDIT_SUBMENU_PREFIX + "-d";
    public final String CALLBACK_ADMIN_REDEEM = "key-redeem";
    public final String CALLBACK_ADMIN_CHANGE_PERSONAL_VENUE = "change-personal-venue";
    public final String CALLBACK_ADMIN_CHANGE_DIET = "change-diet";
    public final String CALLBACK_ADMIN_ABOUT_ME = "about-me";
    public final String CALLBACK_ADMIN_FORGET_ME = "forget-me";
    public final String CALLBACK_ADMIN_GENERATE_KEY = "gen-key";

    List<List<List<InlineKeyboardButton>>> pagedSuperAdminMenu;
    List<List<List<InlineKeyboardButton>>> pagedFullAdminMenu;
    List<List<List<InlineKeyboardButton>>> pagedLimitedAdminMenu;

    HashMap<Long, Long> venuesBeingEditedByUsers = new HashMap<>();

    public AdminMenuServiceImpl(VenueRepository venueRepository, CachedUserRepository cachedUserRepository, AdminKeyRepository adminKeyRepository, UserMenuService userMenuService, InlineKeyboardService inlineKeyboardService, LocalizationService localizationService) {
        this.venueRepository = venueRepository;
        this.cachedUserRepository = cachedUserRepository;
        this.adminKeyRepository = adminKeyRepository;
        this.userMenuService = userMenuService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localizationService = localizationService;
    }

    @Override
    public String getCallbackPrefix() {
        return CALLBACK_PREFIX;
    }

    @Override
    public SendMessage getAdminMenu(CachedUser user) {
        SendMessage reply = new SendMessage(user.getChatId().toString(), localizationService.getString("reply.admin"));
        reply.setReplyMarkup(getAdminMenuMarkup(user));
        return reply;
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {

        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        var sanitizedData = stripCallbackPrefix(query.getData());
        int number = StringUtils.getNumberFromCallbackData(sanitizedData);
        sanitizedData = StringUtils.stripNumberFromCallbackData(sanitizedData);

        switch (sanitizedData) {
            case CALLBACK_ADMIN_VENUES_EDIT:
                throw new UnsupportedOperationException("Not implemented yet!");

            case CALLBACK_ADMIN_VENUES_CREATE:
                bot.execute(handleButtonPressCreateVenue(user));
                break;

            case CALLBACK_ADMIN_GENERATE_KEY:
                bot.execute(handleButtonPressAdminKeyGenerate(user));
                break;

            case CALLBACK_ADMIN_REDEEM:
                handleButtonPressAdminKeyRedemption(user);

                var redeemReply = new SendMessage();
                redeemReply.setChatId(user.getChatId());
                redeemReply.setText(localizationService.getString("admin.sendkey"));
                return redeemReply;

            case CALLBACK_ADMIN_CHANGE_DIET:
                bot.execute(userMenuService.getDietSelection(user));
                break;

            case CALLBACK_ADMIN_CHANGE_PERSONAL_VENUE:
                bot.execute(userMenuService.getVenueSelection(user));
                break;

            case CALLBACK_ADMIN_ABOUT_ME:
                var sendMessage = new SendMessage(user.getChatId().toString(), getAboutData(user));
                sendMessage.setParseMode(ParseMode.MARKDOWNV2);

                bot.execute(sendMessage);
                break;

            case CALLBACK_ADMIN_FORGET_ME:
                cachedUserRepository.delete(user);
                bot.execute(new SendMessage(user.getChatId().toString(), localizationService.getString("reply.forgetme")));
                break;


            case InlineKeyboardService.CALLBACK_NAVIGATION_CLOSE:
                reply.setText(localizationService.getString("admin.closed"));
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
                reply.setText(localizationService.getString("admin.pagepress"));
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
                var editMessage = new EditMessageReplyMarkup();
                editMessage.setChatId(user.getChatId().toString());
                editMessage.setMessageId(query.getMessage().getMessageId());
                editMessage.setReplyMarkup(getAdminMenuMarkup(user, number));

                bot.execute(editMessage);
                break;

            default:
                throw new IllegalArgumentException(String.format("Unknown callback data for admin menu! Given data: %s, CallbackQuery: %s", query.getData(), query));
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        switch (StringUtils.stripNumberFromCallbackData(stripCallbackPrefix(query.getData()))) {
            case CALLBACK_ADMIN_ABOUT_ME:
            case InlineKeyboardService.CALLBACK_NAVIGATION_BACK:
            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
            case CALLBACK_ADMIN_GENERATE_KEY:
            case CALLBACK_ADMIN_VENUES_EDIT:
                return false;

            default:
                return true;
        }
    }

    @Override
    public BotApiMethod<?> handleKeyRedemption(CachedUser user, String key) {
        var reply = new SendMessage();
        reply.setChatId(user.getChatId());
        reply.setText(localizationService.getString("admin.keyredeemed"));

        // don't handle if we're not expecting the user to send us a key
        if (!user.hasState(UserState.SENDING_ADMIN_KEY))
            throw new RuntimeException("No admin key expected!");

        var adminKeyOptional = adminKeyRepository
                .findById(
                        UUID.fromString(key.replaceAll(
                                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                                "$1-$2-$3-$4-$5")));

        if (adminKeyOptional.isEmpty()) {
            throw new RuntimeException(String.format("Couldn't find admin key %s in repository", key));
        }

        var adminKey = adminKeyOptional.get();

        try {
            if (adminKey.isSuperAdminKey())
                user.setAdminKeyAsSuperAdmin(adminKey);
            else
                user.setAdminKey(adminKey);

            user.removeState(UserState.SENDING_ADMIN_KEY);

            cachedUserRepository.saveAndFlush(user);
        } catch (RuntimeException e) {
            logger.error(String.format("Encountered an exception while redeeming admin key for user %s", user), e);
            throw e;
        }

        return reply;
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
        String filePath = bot.execute(new GetFile(fileId)).getFilePath();

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
                Set<String> ingredients = Set.of(row.get(3).trim().split(","));
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

        venue.setPizzaMenu(pizzas);
        venueRepository.saveAndFlush(venue);

        logger.info(String.format("Processed %d rows and generated %d pizza objects, encountered %d errors", rows.size(), pizzas.size(), errorCounter));

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
    public SendMessage handleVenueModificationReply(CachedUser user, String message) {
        if (!user.isAdmin())
            throw new NotAuthorizedException(String.format("User %d is not allowed to modify venues!", user.getChatId()));

        if (!user.hasState(UserState.MODIFYING_VENUE))
            throw new RuntimeException(String.format("User is not in required state %s!", UserState.SENDING_VENUE_NAME));

        var venue = venueRepository.findById(venuesBeingEditedByUsers.get(user.getChatId())).get();

        if (user.hasState(UserState.SENDING_VENUE_NAME)) {
            venue.setName(message.trim());
            user.removeState(UserState.SENDING_VENUE_NAME);
        } else if (user.hasState(UserState.SENDING_VENUE_ADDRESS)) {
            // TODO
        } else if (user.hasState(UserState.SENDING_VENUE_PHONE_NUMBER)) {
            // TODO
        }

        user.removeState(UserState.MODIFYING_VENUE);

        venueRepository.saveAndFlush(venue);
        cachedUserRepository.saveAndFlush(user);

        var text = localizationService.getString("admin.venues.edit.success");
        text = StringUtils.replacePropertiesVariable("name", venue.getName(), text);

        return new SendMessage(user.getChatId().toString(), text);
    }

    private InlineKeyboardMarkup getAdminMenuMarkup(CachedUser user) {
        return getAdminMenuMarkup(user, 0);
    }

    private InlineKeyboardMarkup getAdminMenuMarkup(CachedUser user, int zeroBasedPage) {
        var keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons;

        regenerateMenuCaches();

        if (user.isSuperAdmin()) {
            buttons = pagedSuperAdminMenu.get(zeroBasedPage);
        } else if (user.isAdmin()) {
            buttons = pagedFullAdminMenu.get(zeroBasedPage);
        } else {
            buttons = pagedLimitedAdminMenu.get(zeroBasedPage);
        }

        keyboard.setKeyboard(buttons);
        return keyboard;
    }

    private SendMessage handleButtonPressCreateVenue(CachedUser user) {
        user.addState(UserState.CREATING_VENUE);
        user.addState(UserState.SENDING_VENUE_NAME);

        cachedUserRepository.saveAndFlush(user);

        return new SendMessage(user.getChatId().toString(), localizationService.getString("admin.venues.create.name"));
    }

    private void handleButtonPressAdminKeyRedemption(CachedUser user) {
        user.addState(UserState.SENDING_ADMIN_KEY);

        cachedUserRepository.saveAndFlush(user);
    }

    private SendMessage handleButtonPressAdminKeyGenerate(CachedUser user) {
        if (!user.isSuperAdmin())
            throw new NotAuthorizedException(String.format("User %s is not a super admin!", user.getChatId()));

        var message = new SendMessage();
        var text = localizationService.getString("admin.genkeydone");

        var key = genAdminKey();

        message.setChatId(user.getChatId());
        message.setText(StringUtils.replacePropertiesVariable("key", key.getKeyString(), text));
        message.setParseMode(ParseMode.MARKDOWNV2);

        return message;
    }

    private AdminKey genAdminKey() {
        var key = new AdminKey();

        adminKeyRepository.saveAndFlush(key);

        return key;
    }

    private String getAboutData(CachedUser user) {
        var createdAt = StringUtils.escapeForMarkdownV2Format(user.getCreatedAt().toString());
        var modifiedAt = StringUtils.escapeForMarkdownV2Format(user.getModifiedAt().toString());
        var userState = StringUtils.escapeForMarkdownV2Format(user.getState().toString());

        return String.format("__*User %s*__\nSuperAdmin: %s\nAdmin: %s\nDiet: %s\nSelected venue: %s\nUser states: %s\nFirst seen: _%s_\nLast modified: _%s_", user.getChatId(), user.isSuperAdmin(), user.isAdmin(), user.getUserDiet(), user.getSelectedVenue().getId(), userState, createdAt, modifiedAt);
    }

    private Stream<InlineKeyboardButton> getLimitedAdminMenuButtons() {
        var buttonRedeemKey = new InlineKeyboardButton(localizationService.getString("admin.redeemkey"));
        buttonRedeemKey.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_REDEEM));

        return Stream.concat(Stream.of(buttonRedeemKey), getCommonButtons());
    }

    private Stream<InlineKeyboardButton> getFullAdminMenuButtons() {
        var buttonCreateVenue = new InlineKeyboardButton(localizationService.getString("admin.venues.create"));
        buttonCreateVenue.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_VENUES_CREATE));

        var buttonEditVenues = new InlineKeyboardButton(localizationService.getString("admin.venues.edit"));
        buttonEditVenues.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_VENUES_EDIT));

        return Stream.concat(Stream.of(buttonCreateVenue, buttonEditVenues), getCommonButtons());
    }

    private Stream<InlineKeyboardButton> getSuperAdminMenuButtons() {
        var buttonGenerateAdminKey = new InlineKeyboardButton(localizationService.getString("admin.genkey"));
        buttonGenerateAdminKey.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_GENERATE_KEY));

        return Stream.concat(Stream.of(buttonGenerateAdminKey), getFullAdminMenuButtons());
    }

    private Stream<InlineKeyboardButton> getCommonButtons() {
        var buttonDiet = new InlineKeyboardButton(localizationService.getString("admin.changediet"));
        buttonDiet.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_CHANGE_DIET));

        var buttonPersonalVenue = new InlineKeyboardButton(localizationService.getString("admin.changevenue"));
        buttonPersonalVenue.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_CHANGE_PERSONAL_VENUE));

        var buttonAboutMe = new InlineKeyboardButton(localizationService.getString("admin.aboutme"));
        buttonAboutMe.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_ABOUT_ME));

        var buttonForgetMe = new InlineKeyboardButton(localizationService.getString("admin.forgetme"));
        buttonForgetMe.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_FORGET_ME));

        return Stream.of(buttonDiet, buttonPersonalVenue ,buttonAboutMe, buttonForgetMe);
    }

    private void regenerateMenuCaches() {
        if (pagedSuperAdminMenu == null || pagedSuperAdminMenu.isEmpty())
            pagedSuperAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCloseButton(getSuperAdminMenuButtons().toList(), MENU_COLUMNS, MENU_ROWS, CALLBACK_PREFIX);

        if (pagedFullAdminMenu == null || pagedFullAdminMenu.isEmpty())
            pagedFullAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCloseButton(getFullAdminMenuButtons().toList(), MENU_COLUMNS, MENU_ROWS, CALLBACK_PREFIX);

        if (pagedLimitedAdminMenu == null || pagedLimitedAdminMenu.isEmpty())
            pagedLimitedAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCloseButton(getLimitedAdminMenuButtons().toList(), MENU_COLUMNS, MENU_ROWS, CALLBACK_PREFIX);
    }
}
