package eu.kaesebrot.dev.bot;

import eu.kaesebrot.dev.handler.IUpdateHandler;
import eu.kaesebrot.dev.properties.TelegramBotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.starter.SpringWebhookBot;

@Component
public class PizzaSuggesterBot extends SpringWebhookBot {
    @Value("${telegrambot.token}")
    private String botToken;
    @Autowired
    private IUpdateHandler updateHandler;

    private final TelegramBotProperties properties;

    Logger logger = LoggerFactory.getLogger(PizzaSuggesterBot.class);

    public PizzaSuggesterBot(TelegramBotProperties properties) {
        super(new SetWebhook(properties.getWebhookUrl()), properties.getBotToken());
        this.properties = properties;
    }

    @Override
    public String getBotUsername() {
        return properties.getBotUsername();
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        logger.debug("Got update: {}", update.toString());
        return updateHandler.handleUpdate(update);
    }

    @Override
    public String getBotPath() {
        return "/callback/${telegrambot.token}";
    }
}
