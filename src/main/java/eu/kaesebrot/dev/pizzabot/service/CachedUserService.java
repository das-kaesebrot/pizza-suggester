package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;

public interface CachedUserService {
    CachedUser findOrAddUserByChatId(Long chatId);
}
