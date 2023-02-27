package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.CachedUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface AdminService {
    InlineKeyboardMarkup getAdminMenu(CachedUser user);
}
