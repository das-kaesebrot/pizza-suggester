package eu.kaesebrot.dev.pizzabot.bot;

import eu.kaesebrot.dev.pizzabot.enums.BotCommand;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.model.AdminKey;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.repository.AdminKeyRepository;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.service.CachedUserService;
import eu.kaesebrot.dev.pizzabot.service.CallbackHandlingService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.service.menu.UserMenuService;
import eu.kaesebrot.dev.pizzabot.properties.TelegramBotProperties;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import eu.kaesebrot.dev.pizzabot.service.menu.AdminMenuService;
import eu.kaesebrot.dev.pizzabot.utils.CsvMimeTypeUtil;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;

@Component
public class PizzaSuggesterBot extends SpringWebhookBot {
    Logger logger = LoggerFactory.getLogger(PizzaSuggesterBot.class);
    private final CachedUserService cachedUserService;
    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final AdminMenuService adminMenuService;
    private final UserMenuService userMenuService;
    private final CallbackHandlingService callbackHandlingService;
    private final TelegramBotProperties properties;
    private final LocalizationService localizationService;

    @Value("#{environment.getProperty('debug') != null && environment.getProperty('debug') != 'false'}")
    private boolean isDebug;

    public PizzaSuggesterBot(TelegramBotProperties properties,
                             CachedUserService cachedUserService,
                             CachedUserRepository cachedUserRepository,
                             VenueRepository venueRepository,
                             AdminKeyRepository adminKeyRepository,
                             AdminMenuService adminMenuService,
                             UserMenuService userMenuService,
                             CallbackHandlingService callbackHandlingService, LocalizationService localizationService) throws TelegramApiException {
        super(new SetWebhook(properties.getWebhookBaseUrl()), properties.getBotToken());
        this.properties = properties;
        this.cachedUserService = cachedUserService;
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.adminMenuService = adminMenuService;
        this.userMenuService = userMenuService;
        this.callbackHandlingService = callbackHandlingService;
        this.localizationService = localizationService;

        // set webhook url on startup
        this.setWebhook(this.getSetWebhook());

        // create an initial admin key if the repository is empty
        if (adminKeyRepository.count() <= 0) {
            var firstAdminKey = new AdminKey(true);
            adminKeyRepository.saveAndFlush(firstAdminKey);
            logger.info(String.format("Created an initial super admin key, use this key to gain admin permissions to the bot!\n\n\t*** ADMINKEY: %s ***\n\n", firstAdminKey));
        }
    }

    @Override
    public String getBotUsername() {
        return properties.getBotUsername();
    }

    @Override
    public String getBotPath() {
        return "/" + properties.getBotToken();
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        logger.debug("Got update: {}", update.toString());

        var message = update.getMessage();
        var callbackQuery = update.getCallbackQuery();

        var reply = new SendMessage();

        Long chatId;

        if (!update.hasMessage() && !update.hasCallbackQuery()) {
            logger.error(String.format("Got an invalid update: %s", update));
            throw new RuntimeException("Can't handle update!");
        }

        if (message != null) {
            chatId = message.getChatId();
        } else {
            chatId = callbackQuery.getFrom().getId();
        }

        reply.setChatId(chatId);
        reply.setText(localizationService.getString("error.unknownop"));

        try {
            // don't handle if message doesn't come from a user
            if (update.hasMessage() && !message.isUserMessage()) {
                var leave = new LeaveChat();
                leave.setChatId(chatId);
                return leave; // mommy taught me not to talk to strangers!
            }

            //logger.debug("User has language tag: {}", update.getMessage().getChatId())

            boolean isNew = !cachedUserRepository.existsById(chatId);
            CachedUser user = cachedUserService.findOrAddUserByChatId(chatId);

            // early return to show veggie/meat preference selection and usage instructions
            if (isNew) {
                execute(new SendMessage(user.getChatId().toString(), localizationService.getString("reply.firstrun")));

                if (!venueRepository.findAll().isEmpty()) {
                    execute(userMenuService.getVenueSelection(user));
                }

                execute(userMenuService.getDietSelection(user));

                execute(new SendMessage(user.getChatId().toString(), localizationService.getString("select.disclaimer")));

                return userMenuService.getHelpMessage(user);
            }

            if (callbackQuery != null) {
                logger.debug("Update type: callbackQuery");

                return callbackHandlingService.handleCallback(user, callbackQuery, this);
            }

            if (!update.hasMessage())
                return reply;

            if (update.getMessage().hasDocument() && CsvMimeTypeUtil.MimeTypeCouldBeCsv(update.getMessage().getDocument().getMimeType()))
            {
                return adminMenuService.handleCsvUpload(user, update.getMessage().getDocument().getFileId(), this);
            }

            if (update.hasMessage()) {
                var messageText = message.getText();

                // if the incoming string is 32 chars long, we can assume it is a UUID
                if (messageText.length() == 32) {
                    return adminMenuService.handleKeyRedemption(user, messageText);
                }

                if (messageText.length() > 1 && messageText.startsWith("/")) {
                    logger.debug("Update type: command");
                    messageText = messageText.substring(1);

                    var command = BotCommand.valueOf(messageText.toUpperCase());
                    logger.debug("Mapped command string '{}' to enum type '{}'", messageText, command);

                    if (
                            !(command == BotCommand.SETTINGS)
                            && (user.hasState(UserState.SELECTING_DIET)
                                || user.hasState(UserState.SELECTING_VENUE))
                    )
                    {
                        reply.setText(localizationService.getString("error.finishselection"));
                        return reply;
                    }

                    switch (command) {
                        case START:
                        case HELP:
                            return userMenuService.getHelpMessageWithGreeting(user);

                        case PIZZA:
                            return userMenuService.getIngredientSelectionMenu(user);

                        case RANDOM:
                            return userMenuService.getRandomPizza(user);

                        case SETTINGS:
                            return adminMenuService.getAdminMenu(user);

                        case CARLOS:
                        case CONTACT:
                        case ABOUT:
                            // TODO
                            reply.setText(localizationService.getString("error.notimplemented"));
                            // reply.setReplyMarkup(inlineKeyboardService.getInitialKeyboard(0L));
                            return reply;
                    }

                }
            }

            return reply;

        } catch (Exception e) {
            logger.error("Exception encountered while handling an update", e);
            if (isDebug) {
                var stackTraceZero = e.getStackTrace()[0];
                reply.setText(String.format("Encountered exception:\n```java\n%s\n\tat %s.%s(%s:%d)\n```",
                        StringUtils.escapeCodeForMarkdownV2Format(e.toString()),
                        stackTraceZero.getClassName(), stackTraceZero.getMethodName(), stackTraceZero.getFileName(),
                        stackTraceZero.getLineNumber()));
                reply.setParseMode(ParseMode.MARKDOWNV2);
            } else {
                reply.setText(localizationService.getString("error.generic"));
            }

            return reply;
        }
    }
}
