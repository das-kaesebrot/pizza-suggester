package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.enums.UserState;
import eu.kaesebrot.dev.model.CachedUser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {
    Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);
    private final VenueRepository venueRepository;
    private final CachedUserRepository cachedUserRepository;
    private final AdminKeyRepository adminKeyRepository;
    private HashMap<Long, List<Long>> menuChatMessagesForUser;

    public final String CALLBACK_ADMIN_VENUS = AdminService.CALLBACK_PREFIX + "-edit-venues";
    public final String CALLBACK_ADMIN_REDEEM = AdminService.CALLBACK_PREFIX + "-key-redeem";
    public final String CALLBACK_ADMIN_CLOSE = AdminService.CALLBACK_PREFIX + "-close-menu";

    public AdminServiceImpl(VenueRepository venueRepository, CachedUserRepository cachedUserRepository, AdminKeyRepository adminKeyRepository) {
        this.venueRepository = venueRepository;
        this.cachedUserRepository = cachedUserRepository;
        this.adminKeyRepository = adminKeyRepository;
        menuChatMessagesForUser = new HashMap<>();
    }

    @Override
    public InlineKeyboardMarkup getAdminMenu(CachedUser user) {
        var keyboard = new InlineKeyboardMarkup();

        if (user.isAdmin()) {
            keyboard.setKeyboard(getFullAdminMenu());
            return keyboard;
        }

        keyboard.setKeyboard(getLimitedAdminMenu());
        return keyboard;
    }

    @Override
    public void handleAdminCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {

        switch (query.getData()) {
            case CALLBACK_ADMIN_VENUS:
                // TODO handle button press of admin venues
                break;
            case CALLBACK_ADMIN_REDEEM:
                handleButtonPressAdminKeyRedemption(user);

                var reply = new SendMessage();
                reply.setChatId(user.getChatId());
                reply.setText("Please send me the admin key now");
                // delete the message the menu inline keyboard was attached to
                bot.execute(new DeleteMessage(query.getFrom().getId().toString(), Integer.parseInt(query.getInlineMessageId())));
                bot.execute(reply);
            case CALLBACK_ADMIN_CLOSE:
                // delete the message the menu inline keyboard was attached to
                bot.execute(new DeleteMessage(query.getFrom().getId().toString(), Integer.parseInt(query.getInlineMessageId())));
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown callback data for admin menu! Given data: %s, CallbackQuery: %s", query.getData(), query));
        }
    }

    @Override
    public void handleKeyRedemption(CachedUser user, String key) {
        // don't handle if we're not expecting the user to send us a key
        if (!user.hasState(UserState.SENDING_ADMIN_KEY))
            return;

        var adminKeyOptional = adminKeyRepository
                .findById(
                        UUID.fromString(key.replaceAll(
                                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                                "$1-$2-$3-$4-$5")));

        if (adminKeyOptional.isEmpty()) {
            logger.error(String.format("Couldn't find admin key %s in repository", key));
            return;
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
    }

    @Override
    public void handleCsvUpload(CachedUser user, String fileId, PizzaSuggesterBot bot) throws TelegramApiException {

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
            return;
        }

    }

    private void handleButtonPressAdminKeyRedemption(CachedUser user) {
        user.addState(UserState.SENDING_ADMIN_KEY);

        cachedUserRepository.saveAndFlush(user);
    }

    private List<List<InlineKeyboardButton>> getLimitedAdminMenu() {
        var buttonRedeemKey = new InlineKeyboardButton("Redeem admin key");
        buttonRedeemKey.setCallbackData(CALLBACK_ADMIN_REDEEM);
        var buttonClose = new InlineKeyboardButton("Close");
        buttonClose.setCallbackData(CALLBACK_ADMIN_CLOSE);

        return List.of(List.of(buttonRedeemKey), List.of(buttonClose));
    }

    private List<List<InlineKeyboardButton>> getFullAdminMenu() {
        var buttonVenues = new InlineKeyboardButton("Venues");
        buttonVenues.setCallbackData(CALLBACK_ADMIN_VENUS);
        var buttonClose = new InlineKeyboardButton("Close");
        buttonClose.setCallbackData(CALLBACK_ADMIN_CLOSE);

        return List.of(List.of(buttonVenues), List.of(buttonClose));
    }
}
