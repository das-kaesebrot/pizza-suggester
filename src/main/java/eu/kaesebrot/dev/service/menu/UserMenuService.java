package eu.kaesebrot.dev.service.menu;

import eu.kaesebrot.dev.model.CachedUser;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public interface UserMenuService extends MenuService {
    String CALLBACK_PREFIX = "usermenu";
    SendMessage getDietSelection(CachedUser user);
    SendMessage getVenueSelection(CachedUser user);
}
