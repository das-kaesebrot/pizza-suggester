package eu.kaesebrot.dev.handler;

import eu.kaesebrot.dev.service.AdminService;
import eu.kaesebrot.dev.service.CachedUserRepository;
import eu.kaesebrot.dev.service.IngredientInlineKeyboardService;
import eu.kaesebrot.dev.service.VenueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class UpdateHandlerImpl implements UpdateHandler {
    Logger logger = LoggerFactory.getLogger(UpdateHandlerImpl.class);

    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final IngredientInlineKeyboardService ingredientInlineKeyboardService;
    private final AdminService adminService;

    public enum BotCommand {
        PIZZA,
        RANDOM,
        HELP,
        ABOUT,
        ADMIN,
        CARLOS,
        UNKNOWN
        ;
    }

    public UpdateHandlerImpl(
            CachedUserRepository cachedUserRepository,
            VenueRepository venueRepository,
            IngredientInlineKeyboardService ingredientInlineKeyboardService,
            AdminService adminService)
    {
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.ingredientInlineKeyboardService = ingredientInlineKeyboardService;
        this.adminService = adminService;
    }

    @Override
    public BotApiMethod handleUpdate(Update update) {
        logger.debug("Handling update: " + update.toString());

        var message = update.getMessage();
        var callbackQuery = update.getCallbackQuery();
        var user = cachedUserRepository.findOrAddUserByChatId(message.getChatId());

        var reply = new SendMessage();
        reply.setChatId(message.getChatId());
        reply.setText("Unknown operation, use /help for help");

        if (callbackQuery != null) {
            logger.debug("Update type: callbackQuery");
            // TODO handle callback
        }

        if (update.getMessage() != null) {
            var messageText = message.getText();

            if (messageText.length() > 1 && messageText.charAt(0) == '/') {
                logger.debug("Update type: command");
                messageText = messageText.substring(1);

                var command = mapStringToBotCommand(messageText);
                logger.debug("Mapped command string '{}' to enum type '{}'", messageText, command);

                switch (command) {
                    case PIZZA:
                        // TODO set reply text for pizza selection
                        reply.setText("placeholder text for /pizza command");
                        // reply.setReplyMarkup(inlineKeyboardService.getInitialKeyboard(0L));
                        return reply;

                    case RANDOM:
                        // TODO implement random pizza selection
                        break;

                    case HELP:
                        // TODO set reply text for /help
                        reply.setText("placeholder /help text");
                        return reply;

                    case ADMIN:
                        reply.setText("Placeholder /admin text");
                        reply.setReplyMarkup(adminService.getAdminMenu(user));
                        return reply;

                    case CARLOS:
                        break;
                }

            }
        }

        return reply;
    }

    private BotCommand mapStringToBotCommand(String str) {
        switch (str) {
            case "pizza":
                return BotCommand.PIZZA;

            case "zufall":
                return BotCommand.RANDOM;

            case "start":
            case "help":
                return BotCommand.HELP;

            case "carlos":
                return BotCommand.CARLOS;

            case "admin":
                return BotCommand.ADMIN;

            default:
                return BotCommand.UNKNOWN;
        }
    }
}
