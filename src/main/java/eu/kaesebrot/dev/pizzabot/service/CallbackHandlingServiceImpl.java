package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.service.menu.AdminMenuService;
import eu.kaesebrot.dev.pizzabot.service.menu.MenuService;
import eu.kaesebrot.dev.pizzabot.service.menu.UserMenuService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class CallbackHandlingServiceImpl implements CallbackHandlingService {
    private final AdminMenuService adminMenuService;
    private final UserMenuService userMenuService;

    public CallbackHandlingServiceImpl(AdminMenuService adminMenuService, UserMenuService userMenuService) {
        this.adminMenuService = adminMenuService;
        this.userMenuService = userMenuService;
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {
        BotApiMethod<?> reply;

        MenuService handlingService;

        if (query.getData().startsWith(adminMenuService.CALLBACK_PREFIX)) {
            handlingService = adminMenuService;
        } else if (query.getData().startsWith(userMenuService.CALLBACK_PREFIX)) {
            handlingService = userMenuService;
        } else {
            throw new RuntimeException(String.format("Can't map callback data to service: %s", query.getData()));
        }

        reply = handlingService.handleCallback(user, query, bot);

        // delete the message the menu inline keyboard was attached to after handling
        if (handlingService.canCallbackMenuBeDeletedAfterHandling(query))
            bot.execute(deleteCallbackMessage(query));

        return reply;
    }

    @Override
    public DeleteMessage deleteCallbackMessage(CallbackQuery query) {
        return new DeleteMessage(query.getFrom().getId().toString(), query.getMessage().getMessageId());
    }
}
