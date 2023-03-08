package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.enums.UserState;
import eu.kaesebrot.dev.model.CachedUser;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final VenueRepository venueRepository;
    private final CachedUserRepository cachedUserRepository;
    private HashMap<Long, List<Long>> menuChatMessagesForUser;

    public final String CALLBACK_ADMIN_VENUS = "admin-edit-venues";
    public final String CALLBACK_ADMIN_REDEEM = "admin-key-redeem";
    public final String CALLBACK_ADMIN_CLOSE = "admin-close-menu";

    public AdminServiceImpl(VenueRepository venueRepository, CachedUserRepository cachedUserRepository) {
        this.venueRepository = venueRepository;
        this.cachedUserRepository = cachedUserRepository;
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
    public void handleAdminCallback(CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {

        switch (query.getData()) {
            case CALLBACK_ADMIN_VENUS:
                // TODO handle button press of admin venues
                break;
            case CALLBACK_ADMIN_REDEEM:
                handleButtonPressAdminKeyRedemption(query.getFrom().getId());
                var reply = new SendMessage();
                reply.setChatId(query.getFrom().getId());
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

    private void handleButtonPressAdminKeyRedemption(long chatId) {
        var user = cachedUserRepository.findOrAddUserByChatId(chatId);

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
