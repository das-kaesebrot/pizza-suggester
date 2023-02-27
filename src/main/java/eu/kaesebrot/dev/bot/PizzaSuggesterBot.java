package eu.kaesebrot.dev.bot;

import eu.kaesebrot.dev.properties.TelegramBotProperties;
import eu.kaesebrot.dev.service.AdminService;
import eu.kaesebrot.dev.service.CachedUserRepository;
import eu.kaesebrot.dev.service.IngredientInlineKeyboardService;
import eu.kaesebrot.dev.service.VenueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;

@Component
public class PizzaSuggesterBot extends SpringWebhookBot {
    Logger logger = LoggerFactory.getLogger(PizzaSuggesterBot.class);
    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final IngredientInlineKeyboardService ingredientInlineKeyboardService;
    private final AdminService adminService;
    private final TelegramBotProperties properties;

    public enum BotCommand {
        PIZZA,
        RANDOM,
        HELP,
        ABOUT,
        ADMIN,
        CARLOS,
        START,
        UNKNOWN
        ;
    }

    public PizzaSuggesterBot(TelegramBotProperties properties,
                             CachedUserRepository cachedUserRepository,
                             VenueRepository venueRepository,
                             IngredientInlineKeyboardService ingredientInlineKeyboardService,
                             AdminService adminService)
    {
        super(new SetWebhook(properties.getWebhookUrl()), properties.getBotToken());
        this.properties = properties;
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.ingredientInlineKeyboardService = ingredientInlineKeyboardService;
        this.adminService = adminService;
    }

    @Override
    public String getBotUsername() {
        return properties.getBotUsername();
    }

    @Override
    public String getBotPath() {
        return "/callback/" + properties.getBotToken();
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        logger.debug("Got update: {}", update.toString());

        var message = update.getMessage();
        var callbackQuery = update.getCallbackQuery();

        var reply = new SendMessage();
        reply.setChatId(message.getChatId());
        reply.setText("Unknown operation, use /help for help");

        try {
            // don't handle if message doesn't come from a user
            if (!message.isUserMessage()) {
                var leave = new LeaveChat();
                leave.setChatId(message.getChatId());
                return leave;
            }

            // show as typing to the user
            var typingAction = new SendChatAction();
            typingAction.setAction(ActionType.TYPING);
            typingAction.setChatId(message.getChatId());
            execute(typingAction);

            boolean isNew = cachedUserRepository.existsById(message.getChatId());

            if (isNew) {
                // early return to show veggie/meat preference selection and usage instructions
            }

            var user = cachedUserRepository.findOrAddUserByChatId(message.getChatId());

            if (callbackQuery != null) {
                logger.debug("Update type: callbackQuery");
            }

            if (update.getMessage() != null) {
                var messageText = message.getText();

                if (messageText.length() > 1 && messageText.startsWith("/")) {
                    logger.debug("Update type: command");
                    messageText = messageText.substring(1);

                    var command = mapStringToBotCommand(messageText);
                    logger.debug("Mapped command string '{}' to enum type '{}'", messageText, command);

                    switch (command) {
                        case START:
                            // TODO implement start message
                            break;

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

        } catch (TelegramApiException e) {
            logger.error("Exception encountered while handling an update", e);
            reply.setText("Ups, etwas ist leider schiefgelaufen :(");
            return reply;
        }
    }

    private BotCommand mapStringToBotCommand(String str) {
        switch (str) {
            case "pizza":
                return BotCommand.PIZZA;

            case "zufall":
                return BotCommand.RANDOM;

            case "help":
                return BotCommand.HELP;

            case "carlos":
                return BotCommand.CARLOS;

            case "admin":
                return BotCommand.ADMIN;

            case "start":
                return BotCommand.START;

            default:
                return BotCommand.UNKNOWN;
        }
    }
}
