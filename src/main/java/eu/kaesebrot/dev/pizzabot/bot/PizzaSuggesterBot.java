package eu.kaesebrot.dev.pizzabot.bot;

import eu.kaesebrot.dev.pizzabot.enums.PizzaBotCommand;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.exceptions.*;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.service.AdminKeyService;
import eu.kaesebrot.dev.pizzabot.service.CachedUserService;
import eu.kaesebrot.dev.pizzabot.service.CallbackHandlingService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.service.menu.PizzaMenuService;
import eu.kaesebrot.dev.pizzabot.service.menu.UserMenuService;
import eu.kaesebrot.dev.pizzabot.properties.TelegramBotProperties;
import eu.kaesebrot.dev.pizzabot.service.menu.AdminMenuService;
import eu.kaesebrot.dev.pizzabot.utils.CsvMimeTypeUtil;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.starter.SpringWebhookBot;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.MissingResourceException;

@Component
public class PizzaSuggesterBot extends SpringWebhookBot {
    Logger logger = LoggerFactory.getLogger(PizzaSuggesterBot.class);
    private final CachedUserService cachedUserService;
    private final CachedUserRepository cachedUserRepository;
    private final AdminMenuService adminMenuService;
    private final UserMenuService userMenuService;
    private final PizzaMenuService pizzaMenuService;
    private final CallbackHandlingService callbackHandlingService;
    private final TelegramBotProperties properties;
    private final LocalizationService localizationService;
    private Long handledUpdates;
    private final Timestamp startedAt;

    @Value("#{environment.getProperty('debug') != null && environment.getProperty('debug') != 'false'}")
    private boolean isDebug;

    public PizzaSuggesterBot(TelegramBotProperties properties,
                             CachedUserService cachedUserService,
                             CachedUserRepository cachedUserRepository,
                             AdminMenuService adminMenuService,
                             AdminKeyService adminKeyService, UserMenuService userMenuService,
                             PizzaMenuService pizzaMenuService, CallbackHandlingService callbackHandlingService, LocalizationService localizationService) throws TelegramApiException {

        super(new SetWebhook(properties.getFullWebhookUrl()), properties.getBotToken());

        this.properties = properties;
        this.cachedUserService = cachedUserService;
        this.cachedUserRepository = cachedUserRepository;
        this.adminMenuService = adminMenuService;
        this.userMenuService = userMenuService;
        this.pizzaMenuService = pizzaMenuService;
        this.callbackHandlingService = callbackHandlingService;
        this.localizationService = localizationService;
        this.handledUpdates = 0L;
        this.startedAt = Timestamp.from(Instant.now());

        // set webhook url on startup
        var webhook = this.getSetWebhook();
        this.setWebhook(webhook);
        var webhookInfo = this.getWebhookInfo();

        logger.info(String.format("webhook callback URL was set to %s", webhookInfo.getUrl()));

        // create an initial admin key if the repository is empty
        if (adminKeyService.isKeyRepositoryEmpty()) {
            var firstAdminKey = adminKeyService.generateNewAdminKey(true);
            logger.info(String.format("Created an initial super admin key, use this key to gain admin permissions to the bot!\n\n\t*** ADMINKEY: %s ***\n\n", firstAdminKey));
        }
    }

    @Override
    public void onRegister() {
        super.onRegister();

        var commands = SetMyCommands.builder();

        // ignore carlos/start commands
        var excludedCommands = List.of(PizzaBotCommand.CARLOS, PizzaBotCommand.START);

        for (var commandEnum : PizzaBotCommand.values()) {

            if (excludedCommands.contains(commandEnum))
                continue;

            var commandString = commandEnum.toString().toLowerCase();

            try {
                var commandDescription = localizationService.getString(String.format("command.%s", commandString));

                logger.debug("Adding command {} with description {} to registered commands", commandString, commandDescription);

                commands.command(new BotCommand(commandString, commandString));

            } catch (MissingResourceException e) {
                logger.warn(String.format("Skipping command %s because of missing description", commandString), e);
            }
        }

        try {
            execute(commands.build());
        } catch (TelegramApiException e) {
            logger.warn("Encountered exception while setting commands", e);
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
        logger.debug("Got update: {}", update.getUpdateId());
        logger.trace(update.toString());

        handledUpdates++;

        var message = update.getMessage();
        var callbackQuery = update.getCallbackQuery();

        var reply = new SendMessage();
        reply.setText(localizationService.getString("error.unknownop"));

        Long chatId;

        if (!update.hasMessage() && !update.hasCallbackQuery()) {
            logger.error(String.format("Got an invalid update: %s", update));
            return null;
        }

        if (message != null) {
            chatId = message.getChatId();
        } else {
            chatId = callbackQuery.getFrom().getId();
        }

        reply.setChatId(chatId);
        logger.info("Handling update by user: {}", chatId);

        try {
            // don't handle if message doesn't come from a user
            if (update.hasMessage() && !message.isUserMessage()) {
                logger.info("Discarding update since it's coming from a bot");
                return null;
            }

            //logger.debug("User has language tag: {}", update.getMessage().getChatId())

            boolean isNew = !cachedUserRepository.existsById(chatId);
            CachedUser user = cachedUserService.findOrAddUserByChatId(chatId);

            // early return to show veggie/meat preference selection and usage instructions
            if (isNew) {
                logger.info("User is new, sending onboarding replies.");
                return userMenuService.getSetupMessages(user, this);
            }

            if (callbackQuery != null) {
                logger.info("Update type: callbackQuery");
                logger.debug("Data: {}", callbackQuery.getData());

                return callbackHandlingService.handleCallback(user, callbackQuery, this);
            }

            if (!update.hasMessage())
                return reply;

            if (update.getMessage().hasDocument() && CsvMimeTypeUtil.MimeTypeCouldBeCsv(update.getMessage().getDocument().getMimeType()))
            {
                logger.info("Update type: CSV upload");
                return adminMenuService.handleCsvUpload(user, update.getMessage().getDocument().getFileId(), this);
            }

            if (!update.getMessage().hasText()) {
                return null;
            }

            if (update.hasMessage()) {
                var messageText = message.getText();

                // branch if message is a command
                if (messageText.startsWith("/")) {
                    if (messageText.length() <= 1)
                        throw new RuntimeException(String.format("Command %s is too short!", messageText));

                    logger.info("Update type: command");
                    messageText = messageText.substring(1);

                    var command = PizzaBotCommand.valueOf(messageText.toUpperCase());
                    logger.debug("Mapped command string '{}' to enum type '{}'", messageText, command);

                    if (
                            !(command == PizzaBotCommand.SETTINGS
                                    || command == PizzaBotCommand.ABOUT
                                    || command == PizzaBotCommand.HELP
                                    || command == PizzaBotCommand.START
                                    || command == PizzaBotCommand.DIET
                                    || command == PizzaBotCommand.VENUE)
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

                        case DIET:
                            return userMenuService.getDietSelection(user);

                        case VENUE:
                            return userMenuService.getVenueSelection(user);

                        case ABOUT:
                            return userMenuService.getAboutMessage(user);

                        case CONTACT:
                            return userMenuService.getVenueContactInfoMessages(user, this);

                        case CARLOS:
                            // TODO
                            throw new UnsupportedOperationException("Not implemented yet!");
                        }
                } else if (user.hasState(UserState.SENDING_ADMIN_KEY)) {
                    return adminMenuService.handleKeyRedemption(user, messageText.trim());
                } else if (user.hasState(UserState.CREATING_VENUE)) {
                    return adminMenuService.handleVenueCreationReply(user, messageText);
                } else if (user.hasState(UserState.MODIFYING_VENUE)) {
                    return adminMenuService.handleVenueModificationReply(user, messageText, this);
                } else if (messageText.matches("^.*(?i)pizza time.*$")) {
                    reply.setText("https://www.youtube.com/watch?v=TRgdA9_FsXM");
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

        } catch (NoPizzasYetForVenueException e) {
            logger.error("Exception encountered while handling an update", e);

            reply.setText(localizationService.getString("error.nopizzasyet"));
            return reply;

        } catch (IllegalArgumentException | MalformedDataException e) {
            logger.error("Exception encountered while handling an update", e);

            reply.setText(localizationService.getString("error.illegalargument"));
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

    public Long getHandledUpdates() {
        return handledUpdates;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }
}
