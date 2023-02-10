package eu.kaesebrot.dev.controller;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookCallbackController {
    private final PizzaSuggesterBot bot;

    public WebhookCallbackController(PizzaSuggesterBot bot) {
        this.bot = bot;
    }

    @RequestMapping(value = "/callback/${telegrambot.token}", method = RequestMethod.POST)
    @ResponseBody
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return bot.onWebhookUpdateReceived(update);
    }
}
