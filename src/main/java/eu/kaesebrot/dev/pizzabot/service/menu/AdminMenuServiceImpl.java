package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.repository.AdminKeyRepository;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
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
    private final LocalizationService localizationService;
    private HashMap<Long, List<Long>> menuChatMessagesForUser;

    public final String CALLBACK_ADMIN_VENUS = AdminMenuService.CALLBACK_PREFIX + "-edit-venues";
    public final String CALLBACK_ADMIN_REDEEM = AdminMenuService.CALLBACK_PREFIX + "-key-redeem";
    public final String CALLBACK_ADMIN_CHANGE_PERSONAL_VENUE = AdminMenuService.CALLBACK_PREFIX + "-change-personal-venue";
    public final String CALLBACK_ADMIN_CHANGE_DIET = AdminMenuService.CALLBACK_PREFIX + "-change-diet";
    public final String CALLBACK_ADMIN_ABOUT_ME = AdminMenuService.CALLBACK_PREFIX + "-about-me";
    public final String CALLBACK_ADMIN_FORGET_ME = AdminMenuService.CALLBACK_PREFIX + "-forget-me";
    public final String CALLBACK_ADMIN_CLOSE = AdminMenuService.CALLBACK_PREFIX + "-close-menu";

    public AdminMenuServiceImpl(VenueRepository venueRepository, CachedUserRepository cachedUserRepository, AdminKeyRepository adminKeyRepository, UserMenuService userMenuService, LocalizationService localizationService) {
        this.venueRepository = venueRepository;
        this.cachedUserRepository = cachedUserRepository;
        this.adminKeyRepository = adminKeyRepository;
        this.userMenuService = userMenuService;
        this.localizationService = localizationService;
        menuChatMessagesForUser = new HashMap<>();
    }

    @Override
    public InlineKeyboardMarkup getAdminMenu(CachedUser user) {
        var keyboard = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> baseButtons;

        if (user.isAdmin()) {
            baseButtons = getFullAdminMenu();
        } else {
            baseButtons = getLimitedAdminMenu();
        }

        var keyboardButtons = Stream.concat(baseButtons.stream(), getCommonButtons().stream()).toList();

        keyboard.setKeyboard(keyboardButtons);
        return keyboard;
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {

        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        switch (query.getData()) {
            case CALLBACK_ADMIN_VENUS:
                // TODO handle button press of admin venues
                reply.setText(localizationService.getString("error.notimplemented"));
                break;

            case CALLBACK_ADMIN_REDEEM:
                handleButtonPressAdminKeyRedemption(user);

                var redeemReply = new SendMessage();
                redeemReply.setChatId(user.getChatId());
                redeemReply.setText(localizationService.getString("admin.sendkey"));
                return redeemReply;

            case CALLBACK_ADMIN_CLOSE:
                reply.setText(localizationService.getString("admin.closed"));
                break;

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

            default:
                throw new IllegalArgumentException(String.format("Unknown callback data for admin menu! Given data: %s, CallbackQuery: %s", query.getData(), query));
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        switch (query.getData()) {
            case CALLBACK_ADMIN_ABOUT_ME:
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

    private List<List<InlineKeyboardButton>> getLimitedAdminMenu() {
        var buttonRedeemKey = new InlineKeyboardButton(localizationService.getString("admin.redeemkey"));
        buttonRedeemKey.setCallbackData(CALLBACK_ADMIN_REDEEM);

        return List.of(List.of(buttonRedeemKey));
    }

    private List<List<InlineKeyboardButton>> getFullAdminMenu() {
        var buttonVenues = new InlineKeyboardButton(localizationService.getString("admin.venues"));
        buttonVenues.setCallbackData(CALLBACK_ADMIN_VENUS);

        return List.of(List.of(buttonVenues));
    }

    private List<List<InlineKeyboardButton>> getCommonButtons() {
        var buttonDiet = new InlineKeyboardButton(localizationService.getString("admin.changediet"));
        buttonDiet.setCallbackData(CALLBACK_ADMIN_CHANGE_DIET);

        var buttonPersonalVenue = new InlineKeyboardButton(localizationService.getString("admin.changevenue"));
        buttonPersonalVenue.setCallbackData(CALLBACK_ADMIN_CHANGE_PERSONAL_VENUE);

        var buttonAboutMe = new InlineKeyboardButton(localizationService.getString("admin.aboutme"));
        buttonAboutMe.setCallbackData(CALLBACK_ADMIN_ABOUT_ME);

        var buttonForgetMe = new InlineKeyboardButton(localizationService.getString("admin.forgetme"));
        buttonForgetMe.setCallbackData(CALLBACK_ADMIN_FORGET_ME);

        return List.of(
                List.of(buttonDiet),
                List.of(buttonPersonalVenue),
                List.of(buttonAboutMe),
                List.of(buttonForgetMe),
                List.of(getCloseButton())
        );
    }

    private InlineKeyboardButton getCloseButton() {
        var buttonClose = new InlineKeyboardButton(localizationService.getString("label.close"));
        buttonClose.setCallbackData(CALLBACK_ADMIN_CLOSE);

        return buttonClose;
    }
}
