package eu.kaesebrot.dev.pizzabot.service;

public interface LocalizationService {
    String getString(String key);
    String getString(String key, String languageTag);
}
