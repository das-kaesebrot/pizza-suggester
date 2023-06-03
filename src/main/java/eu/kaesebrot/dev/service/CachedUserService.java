package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.CachedUser;

public interface CachedUserService {
    CachedUser findOrAddUserByChatId(Long chatId);
}
