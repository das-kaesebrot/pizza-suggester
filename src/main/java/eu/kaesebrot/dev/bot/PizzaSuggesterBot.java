package eu.kaesebrot.dev.bot;

import eu.kaesebrot.dev.handler.IUpdateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class PizzaSuggesterBot extends TelegramLongPollingBot {
    @Autowired
    private IUpdateHandler updateHandler;

    Logger logger = LoggerFactory.getLogger(PizzaSuggesterBot.class);

    @Value("${telegrambot.username}")
    private String tgBotUsername;

    @Value("${telegrambot.token}")
    private String tgBotToken;

    @Override
    public void onUpdateReceived(Update update) {
        logger.debug("Got update: " + update.toString());
        updateHandler.handleUpdate(update);
    }

    @Override
    public String getBotUsername() {
        return tgBotUsername;
    }

    @Override
    public String getBotToken() {
        return tgBotToken;
    }
}
