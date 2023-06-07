package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.repository.AdminKeyRepository;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import eu.kaesebrot.dev.pizzabot.service.InlineKeyboardService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
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

    public final String CALLBACK_ADMIN_VENUS = "edit-venues";
    public final String CALLBACK_ADMIN_REDEEM = "key-redeem";
    public final String CALLBACK_ADMIN_CHANGE_PERSONAL_VENUE = "change-personal-venue";
    public final String CALLBACK_ADMIN_CHANGE_DIET = "change-diet";
    public final String CALLBACK_ADMIN_ABOUT_ME = "about-me";
    public final String CALLBACK_ADMIN_FORGET_ME = "forget-me";
    public final String CALLBACK_ADMIN_GENERATE_KEY = "gen-key";

    List<List<List<InlineKeyboardButton>>> pagedSuperAdminMenu;
    List<List<List<InlineKeyboardButton>>> pagedFullAdminMenu;
    List<List<List<InlineKeyboardButton>>> pagedLimitedAdminMenu;

    public AdminMenuServiceImpl(VenueRepository venueRepository, CachedUserRepository cachedUserRepository, AdminKeyRepository adminKeyRepository, UserMenuService userMenuService, InlineKeyboardService inlineKeyboardService, LocalizationService localizationService) {
        this.venueRepository = venueRepository;
        this.cachedUserRepository = cachedUserRepository;
        this.adminKeyRepository = adminKeyRepository;
        this.userMenuService = userMenuService;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localizationService = localizationService;
    }

    @Override
    public InlineKeyboardMarkup getAdminMenu(CachedUser user) {
        return getAdminMenu(user, 0);
    }

    @Override
    public InlineKeyboardMarkup getAdminMenu(CachedUser user, long zeroBasedPage) {
        var keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons;

        regenerateMenuCaches();

        if (user.isSuperAdmin()) {
            buttons = pagedSuperAdminMenu.get((int) zeroBasedPage);
        } else if (user.isAdmin()) {
            buttons = pagedFullAdminMenu.get((int) zeroBasedPage);
        } else {
            buttons = pagedLimitedAdminMenu.get((int) zeroBasedPage);
        }

        keyboard.setKeyboard(buttons);
        return keyboard;
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {

        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        var sanitizedData = stripCallbackPrefix(query.getData());

        switch (sanitizedData) {
            case CALLBACK_ADMIN_VENUS:
            case CALLBACK_ADMIN_GENERATE_KEY:
                // TODO handle button press of admin venues
                reply.setText(localizationService.getString("error.notimplemented"));
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

            default:
                throw new IllegalArgumentException(String.format("Unknown callback data for admin menu! Given data: %s, CallbackQuery: %s", query.getData(), query));
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        switch (stripCallbackPrefix(query.getData())) {
            case CALLBACK_ADMIN_ABOUT_ME:
            case InlineKeyboardService.CALLBACK_NAVIGATION_FORWARD:
            case InlineKeyboardService.CALLBACK_NAVIGATION_BACK:
            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
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
            throw new RuntimeException(String.format("No admin key expected!"));

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
        var reply = new SendMessage();
        reply.setChatId(user.getChatId());
        reply.setText(localizationService.getString("admin.csvuploadsuccess"));

        List<List<String>> rows = new java.util.ArrayList<>(List.of());

        logger.debug(String.format("Handling new document by %s: %s", user , fileId));
        String filePath = bot.execute(new GetFile(fileId)).getFilePath();

        var outputStream = new ByteArrayOutputStream();

        try {
            InputStream in = new URL(filePath).openStream();
            IOUtils.copy(in, outputStream);

            String csvContent = outputStream.toString(StandardCharsets.UTF_8);

            try (BufferedReader br = new BufferedReader(new StringReader(csvContent))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");

                    rows.add(Arrays.asList(values));
                }
            }

        } catch (IOException e) {
            logger.error(String.format("Encountered an exception while handling file from path '%s'", filePath), e);
            throw e;
        }

        return reply;
    }

    private void handleButtonPressAdminKeyRedemption(CachedUser user) {
        user.addState(UserState.SENDING_ADMIN_KEY);

        cachedUserRepository.saveAndFlush(user);
    }

    private String getAboutData(CachedUser user) {
        var createdAt = user.getCreatedAt().toString().replace("-", "\\-").replace(".", "\\.");
        var modifiedAt = user.getModifiedAt().toString().replace("-", "\\-").replace(".", "\\.");
        var userState = user.getState().toString().replace("_", "\\_");

        return String.format("__*User %s*__\nAdmin: %s\nDiet: %s\nSelected venue: %s\nUser states: %s\nFirst seen: _%s_\nLast modified: _%s_", user.getChatId(), user.isAdmin(), user.getUserDiet(), user.getSelectedVenue(), userState, createdAt, modifiedAt);
    }

    private Stream<InlineKeyboardButton> getLimitedAdminMenu() {
        var buttonRedeemKey = new InlineKeyboardButton(localizationService.getString("admin.redeemkey"));
        buttonRedeemKey.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_REDEEM));

        return Stream.concat(Stream.of(buttonRedeemKey), getCommonButtons());
    }

    private Stream<InlineKeyboardButton> getFullAdminMenu() {
        var buttonVenues = new InlineKeyboardButton(localizationService.getString("admin.venues"));
        buttonVenues.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_VENUS));

        return Stream.concat(Stream.of(buttonVenues), getCommonButtons());
    }

    private Stream<InlineKeyboardButton> getSuperAdminMenu() {
        var buttonGenerateAdminKey = new InlineKeyboardButton(localizationService.getString("admin.genkey"));
        buttonGenerateAdminKey.setCallbackData(prependCallbackPrefix(CALLBACK_ADMIN_GENERATE_KEY));

        return Stream.concat(Stream.of(buttonGenerateAdminKey), getFullAdminMenu());
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
            pagedSuperAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtons(getSuperAdminMenu().toList(), MENU_COLUMNS, MENU_ROWS, true, CALLBACK_PREFIX, false, true);

        if (pagedFullAdminMenu == null || pagedFullAdminMenu.isEmpty())
            pagedFullAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtons(getFullAdminMenu().toList(), MENU_COLUMNS, MENU_ROWS, true, CALLBACK_PREFIX, false, true);

        if (pagedLimitedAdminMenu == null || pagedLimitedAdminMenu.isEmpty())
            pagedLimitedAdminMenu = inlineKeyboardService.getPagedInlineKeyboardButtons(getLimitedAdminMenu().toList(), MENU_COLUMNS, MENU_ROWS, true, CALLBACK_PREFIX, false, true);
    }

    private String stripCallbackPrefix(String data) {
        return data.replace(String.format("%s-", CALLBACK_PREFIX), "");
    }

    private String prependCallbackPrefix(String data) {
        return String.format("%s-%s", CALLBACK_PREFIX, data);
    }
}
