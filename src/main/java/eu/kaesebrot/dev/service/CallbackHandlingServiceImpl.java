package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.model.CachedUser;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class CallbackHandlingServiceImpl implements CallbackHandlingService {
    private final AdminService adminService;
    private final UserMenuService userMenuService;

    public CallbackHandlingServiceImpl(AdminService adminService, UserMenuService userMenuService) {
        this.adminService = adminService;
        this.userMenuService = userMenuService;
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {
        BotApiMethod<?> reply;
        boolean deleteAfterHandling = true;

        if (query.getData().startsWith(adminService.CALLBACK_PREFIX)) {
            reply = adminService.handleAdminCallback(user, query, bot);
            deleteAfterHandling = adminService.deleteCallbackMenuAfterHandling(query);
        } else if (query.getData().startsWith(userMenuService.CALLBACK_PREFIX)) {
            reply = userMenuService.handleUserMenuCallback(user, query, bot);
        } else {
            throw new RuntimeException(String.format("Can't map callback data to service: %s", query.getData()));
        }

        // delete the message the menu inline keyboard was attached to after handling
        if (deleteAfterHandling)
            bot.execute(deleteCallbackMessage(query));

        return reply;
    }

    @Override
    public DeleteMessage deleteCallbackMessage(CallbackQuery query) {
        return new DeleteMessage(query.getFrom().getId().toString(), query.getMessage().getMessageId());
    }
}
