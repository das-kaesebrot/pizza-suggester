package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;

public interface LocalizationService {
    String getString(String key);
    String getString(String key, String languageTag);
    String getString(String key, CachedUser user);
    String replaceVariable(String variableName, String variableContent, String formattedString);
}
