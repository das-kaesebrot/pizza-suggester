package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.properties.TelegramBotProperties;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.ResourceBundle;

@Service
public class LocalizationServiceImpl implements LocalizationService {
    private static final String BASE_NAME = "messages";
    private final String defaultLocale;

    public LocalizationServiceImpl(TelegramBotProperties properties) {
        defaultLocale = properties.getPrimaryLocale();
    }

    @Override
    public String getString(String key) {
        return ResourceBundle
                .getBundle(BASE_NAME, Locale.forLanguageTag(defaultLocale))
                .getString(key);
    }

    @Override
    public String getString(String key, CachedUser user) {
        var langTag = user.getLanguageTag();

        if (langTag == null || langTag.isBlank() || langTag.isEmpty())
            langTag = defaultLocale;

        return getString(key, langTag);
    }

    @Override
    public String getString(String key, String languageTag) {
        return ResourceBundle
                .getBundle(BASE_NAME, Locale.forLanguageTag(languageTag))
                .getString(key);
    }

    @Override
    public String replaceVariable(String variableName, String variableContent, String formattedString) {
        return formattedString.replace(String.format("$%s", variableName), variableContent);
    }
}
