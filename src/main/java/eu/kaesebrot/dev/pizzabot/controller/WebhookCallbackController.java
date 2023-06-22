package eu.kaesebrot.dev.pizzabot.controller;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookCallbackController {
    private final PizzaSuggesterBot bot;

    public WebhookCallbackController(PizzaSuggesterBot bot) {
        this.bot = bot;
    }

    @RequestMapping(value = "${telegrambot.webhookPath:}/callback/${telegrambot.botToken}", method = RequestMethod.POST)
    @ResponseBody
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        return bot.onWebhookUpdateReceived(update);
    }
}
