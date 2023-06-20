package eu.kaesebrot.dev.pizzabot.bot;

import eu.kaesebrot.dev.pizzabot.enums.BotCommand;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.exceptions.NoPizzasFoundException;
import eu.kaesebrot.dev.pizzabot.exceptions.NotAuthorizedException;
import eu.kaesebrot.dev.pizzabot.exceptions.PendingSelectionException;
import eu.kaesebrot.dev.pizzabot.model.AdminKey;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.repository.AdminKeyRepository;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.service.CachedUserService;
import eu.kaesebrot.dev.pizzabot.service.CallbackHandlingService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.service.menu.PizzaMenuService;
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
    private final PizzaMenuService pizzaMenuService;
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
                             PizzaMenuService pizzaMenuService, CallbackHandlingService callbackHandlingService, LocalizationService localizationService) throws TelegramApiException {
        super(new SetWebhook(properties.getWebhookBaseUrl()), properties.getBotToken());
        this.properties = properties;
        this.cachedUserService = cachedUserService;
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.adminMenuService = adminMenuService;
        this.userMenuService = userMenuService;
        this.pizzaMenuService = pizzaMenuService;
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
                return userMenuService.getSetupMessages(user, this);
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

                // branch if message is a command
                if (messageText.startsWith("/")) {
                    if (messageText.length() <= 1)
                        throw new RuntimeException(String.format("Command %s is too short!", messageText));

                    logger.debug("Update type: command");
                    messageText = messageText.substring(1);

                    var command = BotCommand.valueOf(messageText.toUpperCase());
                    logger.debug("Mapped command string '{}' to enum type '{}'", messageText, command);

                    if (
                            !(command == BotCommand.SETTINGS || command == BotCommand.ABOUT || command == BotCommand.HELP)
                                    && (user.hasState(UserState.SELECTING_DIET)
                                    || user.hasState(UserState.SELECTING_VENUE))
                    ) {
                        throw new PendingSelectionException("User hasn't finished setup yet!");
                    }

                    switch (command) {
                        case START:
                        case HELP:
                            return userMenuService.getHelpMessageWithGreeting(user);

                        case PIZZA:
                            return pizzaMenuService.getIngredientSelectionMenu(user);

                        case RANDOM:
                            return pizzaMenuService.getRandomPizza(user);

                        case SETTINGS:
                            return adminMenuService.getAdminMenu(user);

                        case ABOUT:
                            return userMenuService.getAboutMessage(user);

                        case CARLOS:
                        case CONTACT:
                            // TODO
                            throw new UnsupportedOperationException("Not implemented yet!");
                        }
                } else if (messageText.length() == 32 && user.hasState(UserState.SENDING_ADMIN_KEY)) {
                    return adminMenuService.handleKeyRedemption(user, messageText);
                } else if (user.hasState(UserState.CREATING_VENUE)) {
                    return adminMenuService.handleVenueCreationReply(user, messageText);
                } else if (user.hasState(UserState.MODIFYING_VENUE)) {
                    return adminMenuService.handleVenueModificationReply(user, messageText, this);
                }
            }

            return reply;
        } catch (NotAuthorizedException e) {
            logger.error("Exception encountered while handling an update", e);

            reply.setText(localizationService.getString("error.unauthorized"));
            return reply;

        } catch (UnsupportedOperationException e) {
            logger.error("Exception encountered while handling an update", e);

            reply.setText(localizationService.getString("error.notimplemented"));
            return reply;

        } catch (PendingSelectionException e) {
            logger.error("Exception encountered while handling an update", e);

            reply.setText(localizationService.getString("error.finishselection"));
            return reply;

        } catch (NoPizzasFoundException e) {
            logger.error("Exception encountered while handling an update", e);

            reply.setText(localizationService.getString("error.nopizzasfound"));
            return reply;

        } catch (Exception e) {
            logger.error("Exception encountered while handling an update", e);
            if (isDebug) {
                var stackTraceZero = e.getStackTrace()[0];
                reply.setText(String.format("Encountered unhandled exception:\n```java\n%s\n\tat %s.%s(%s:%d)\n```",
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
