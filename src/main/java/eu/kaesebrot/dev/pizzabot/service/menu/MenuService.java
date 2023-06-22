package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface MenuService {

    BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException;
    boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query);
    String getCallbackPrefix();

    default String stripCallbackPrefix(String data) {
        return StringUtils.stripCallbackPrefix(getCallbackPrefix(), data);
    }

    default String prependCallbackPrefix(String data) {
        return StringUtils.prependCallbackPrefix(getCallbackPrefix(), data);
    }
}
