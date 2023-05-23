package eu.kaesebrot.dev.service;

public interface LocalizationService {
    String getString(String key);
    String getString(String key, String languageTag);
}
