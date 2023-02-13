package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.CachedUser;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.HashMap;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final VenueRepository venueRepository;
    private final CachedUserRepository cachedUserRepository;

    private HashMap<Long, List<Long>> menuChatMessagesForUser;

    public AdminServiceImpl(VenueRepository venueRepository, CachedUserRepository cachedUserRepository) {
        this.venueRepository = venueRepository;
        this.cachedUserRepository = cachedUserRepository;
        menuChatMessagesForUser = new HashMap<>();
    }

    @Override
    public InlineKeyboardMarkup getAdminMenu(CachedUser user) {
        var keyboard = new InlineKeyboardMarkup();
        if (!user.isAdmin()) {
            keyboard.setKeyboard(getLimitedAdminMenu());
        }

        return keyboard;
    }

    private List<List<InlineKeyboardButton>> getLimitedAdminMenu() {
        var buttonRedeemKey = new InlineKeyboardButton("Redeem admin key");
        buttonRedeemKey.setCallbackData("redeem");
        var buttonClose = new InlineKeyboardButton("Close");
        buttonRedeemKey.setCallbackData("close");

        return List.of(List.of(buttonRedeemKey), List.of(buttonClose));
    }
}
