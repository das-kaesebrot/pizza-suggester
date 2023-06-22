package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;

public interface AdminKeyService {
    String generateNewAdminKey(boolean grantsSuperAdminRights);
    boolean isKeyRepositoryEmpty();
    void redeemAdminKeyForUser(CachedUser user, String rawKey);
}
