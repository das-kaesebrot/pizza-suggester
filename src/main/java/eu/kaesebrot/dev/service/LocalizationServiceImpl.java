package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.properties.TelegramBotProperties;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.ResourceBundle;

@Service
public class LocalizationServiceImpl implements LocalizationService {
    private static final String BASE_NAME = "messages";
    private final ResourceBundle bundle;

    public LocalizationServiceImpl(TelegramBotProperties properties) {
        this.bundle = ResourceBundle.getBundle(BASE_NAME, Locale.forLanguageTag(properties.getPrimaryLocale()));
    }

    @Override
    public String getString(String key) {
        return bundle.getString(key);
    }

    @Override
    public String getString(String key, String languageTag) {
        return ResourceBundle
                .getBundle(BASE_NAME, Locale.forLanguageTag(languageTag))
                .getString(key);
    }
}
