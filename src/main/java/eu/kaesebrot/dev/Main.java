package eu.kaesebrot.dev;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new PizzaSuggesterBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}