package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.exceptions.NotAuthorizedException;
import eu.kaesebrot.dev.pizzabot.model.AdminKey;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.properties.TelegramBotProperties;
import eu.kaesebrot.dev.pizzabot.repository.AdminKeyRepository;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.repository.PizzaRepository;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import eu.kaesebrot.dev.pizzabot.service.InlineKeyboardService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinAllChatMessages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class AdminMenuServiceImpl implements AdminMenuService {
    Logger logger = LoggerFactory.getLogger(AdminMenuServiceImpl.class);
    private final VenueRepository venueRepository;
    private final CachedUserRepository cachedUserRepository;
    private final AdminKeyRepository adminKeyRepository;
    private final PizzaRepository pizzaRepository;
    private final UserMenuService userMenuService;
    private final VenueEditSubMenuService venueEditSubMenuService;
    private final InlineKeyboardService inlineKeyboardService;
    private final LocalizationService localizationService;
    private final TelegramBotProperties botProperties;

    private List<List<List<InlineKeyboardButton>>> pagedSuperAdminMenu;
    private List<List<List<InlineKeyboardButton>>> pagedFullAdminMenu;
    private List<List<List<InlineKeyboardButton>>> pagedLimitedAdminMenu;

    public AdminMenuServiceImpl(VenueRepository venueRepository, CachedUserRepository cachedUserRepository, AdminKeyRepository adminKeyRepository, PizzaRepository pizzaRepository, UserMenuService userMenuService, VenueEditSubMenuService venueEditSubMenuService, InlineKeyboardService inlineKeyboardService, LocalizationService localizationService, TelegramBotProperties botProperties) {
        this.venueRepository = venueRepository;
        this.cachedUserRepository = cachedUserRepository;
        this.adminKeyRepository = adminKeyRepository;
        this.pizzaRepository = pizzaRepository;
        this.userMenuService = userMenuService;
        this.venueEditSubMenuService = venueEditSubMenuService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localizationService = localizationService;
        this.botProperties = botProperties;
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

        if (sanitizedData.startsWith(VenueEditSubMenuService.CALLBACK_PREFIX)
                && !sanitizedData.matches(VenueEditSubMenuService.CALLBACK_PREFIX + "-" + InlineKeyboardService.CALLBACK_NAVIGATION_BACK))
            return venueEditSubMenuService.handleCallback(user, query, bot);

        switch (sanitizedData) {
            case VenueEditSubMenuService.CALLBACK_PREFIX + "-" + InlineKeyboardService.CALLBACK_NAVIGATION_BACK:
                handleButtonPressBackFromVenueEditMenu(user, query.getMessage().getMessageId(), bot);
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
                
            case CALLBACK_ADMIN_RESET_INFOPIN:
                userMenuService.resetPinnedInfoMessage(user, bot);
                break;
                
            case CALLBACK_ADMIN_BOTSTATS:
                bot.execute(getBotStatsMessage(user, bot));
                break;

            case CALLBACK_ADMIN_FORGET_ME:
                cachedUserRepository.delete(user);
                bot.execute(new UnpinAllChatMessages(user.getChatId().toString()));
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
        var sanitizedData = StringUtils.stripNumberFromCallbackData(stripCallbackPrefix(query.getData()));

        if (sanitizedData.startsWith(VenueEditSubMenuService.CALLBACK_PREFIX))
            return venueEditSubMenuService.canCallbackMenuBeDeletedAfterHandling(query);

        switch (sanitizedData) {
            case CALLBACK_ADMIN_ABOUT_ME:
            case CALLBACK_ADMIN_GENERATE_KEY:
            case CALLBACK_ADMIN_VENUES_EDIT:
            case CALLBACK_ADMIN_VENUES_CREATE:
            case CALLBACK_ADMIN_RESET_INFOPIN:
            case CALLBACK_ADMIN_BOTSTATS:
            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
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
        return venueEditSubMenuService.handleCsvUpload(user, fileId, bot);
    }

    @Override
    public SendMessage handleVenueCreationReply(CachedUser user, String message) {
        return venueEditSubMenuService.handleVenueCreationReply(user, message);
    }

    @Override
    public SendMessage handleVenueModificationReply(CachedUser user, String message, PizzaSuggesterBot bot) {
        return venueEditSubMenuService.handleVenueModificationReply(user, message, bot);
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

    private void handleButtonPressAdminKeyRedemption(CachedUser user) {
        user.addState(UserState.SENDING_ADMIN_KEY);

        cachedUserRepository.save(user);
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

    private void handleButtonPressBackFromVenueEditMenu(CachedUser user, int messageId, PizzaSuggesterBot bot) throws TelegramApiException {
        var editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(user.getChatId().toString());
        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setReplyMarkup(getAdminMenuMarkup(user, 0));

        var editMessageText = new EditMessageText();
        editMessageText.setChatId(user.getChatId());
        editMessageText.setMessageId(messageId);
        editMessageText.setText(localizationService.getString("reply.admin"));

        bot.execute(editMessageText);
        bot.execute(editMessageReplyMarkup);
    }

    private SendMessage getBotStatsMessage(CachedUser user, PizzaSuggesterBot bot) {
        if (!user.isAdmin())
            throw new NotAuthorizedException(String.format("User %s is not an admin!", user.getChatId()));

        return new SendMessage(user.getChatId().toString(), getBotStats(bot));
    }

    //region helper methods

    private String getAboutData(CachedUser user) {
        var createdAt = StringUtils.escapeForMarkdownV2Format(user.getCreatedAt().toString());
        var modifiedAt = StringUtils.escapeForMarkdownV2Format(user.getModifiedAt().toString());
        var userState = StringUtils.escapeForMarkdownV2Format(user.getState().toString());
        var venue = user.getSelectedVenue();
        var venueNumber = "";

        if (venue != null)
            venueNumber = venue.getId().toString();

        return String.format("__*User %s*__\nSuperAdmin: %s\nAdmin: %s\nDiet: %s\nSelected venue: %s\nUser states: %s\nFirst seen: _%s_\nLast modified: _%s_", user.getChatId(), user.isSuperAdmin(), user.isAdmin(), user.getUserDiet(), venueNumber, userState, createdAt, modifiedAt);
    }

    private String getBotStats(PizzaSuggesterBot bot) {
        return String.format("Handled updates: %d\nCached users: %d\nVenues: %d\nTotal admin keys: %d\nUnredeemed admin keys: %d\nRunning since: %s", bot.getHandledUpdates(), cachedUserRepository.count(), venueRepository.count(), adminKeyRepository.count(), adminKeyRepository.findAll().stream().filter(s -> !s.hasBeenClaimed()).count(), bot.getStartedAt());
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

        var buttonBotStats = new InlineKeyboardButton(localizationService.getString("admin.botstats"));
        buttonBotStats.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_BOTSTATS));

        return Stream.concat(Stream.of(buttonCreateVenue, buttonEditVenues, buttonBotStats), getCommonButtons());
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

        var buttonResetPinnedMessage = new InlineKeyboardButton(localizationService.getString("admin.resetinfopin"));
        buttonResetPinnedMessage.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_RESET_INFOPIN));

        var buttonAboutMe = new InlineKeyboardButton(localizationService.getString("admin.aboutme"));
        buttonAboutMe.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_ABOUT_ME));

        var buttonForgetMe = new InlineKeyboardButton(localizationService.getString("admin.forgetme"));
        buttonForgetMe.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_FORGET_ME));

        return Stream.of(buttonDiet, buttonPersonalVenue, buttonResetPinnedMessage, buttonAboutMe, buttonForgetMe);
    }

    private void regenerateMenuCaches() {
        if (pagedSuperAdminMenu == null || pagedSuperAdminMenu.isEmpty())
            pagedSuperAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCloseButton(getSuperAdminMenuButtons().toList(), MENU_COLUMNS, MENU_ROWS, CALLBACK_PREFIX);

        if (pagedFullAdminMenu == null || pagedFullAdminMenu.isEmpty())
            pagedFullAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCloseButton(getFullAdminMenuButtons().toList(), MENU_COLUMNS, MENU_ROWS, CALLBACK_PREFIX);

        if (pagedLimitedAdminMenu == null || pagedLimitedAdminMenu.isEmpty())
            pagedLimitedAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCloseButton(getLimitedAdminMenuButtons().toList(), MENU_COLUMNS, MENU_ROWS, CALLBACK_PREFIX);
    }

    //endregion
}
