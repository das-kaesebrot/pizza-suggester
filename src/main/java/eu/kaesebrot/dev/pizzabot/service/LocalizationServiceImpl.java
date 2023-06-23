package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.properties.TelegramBotProperties;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
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
        return getString(key, defaultLocale);
    }

    @Override
    public String getString(String key, CachedUser user) {
        var langTag = user.getLanguageTag();

        if (StringUtils.isNullOrEmpty(langTag))
            langTag = defaultLocale;

        return getString(key, langTag);
    }

    @Override
    public String getString(String key, String languageTag) {
        if (ResourceBundle.getBundle(BASE_NAME, Locale.forLanguageTag(languageTag)).containsKey(key))
            return ResourceBundle
                    .getBundle(BASE_NAME, Locale.forLanguageTag(languageTag))
                    .getString(key);

        return ResourceBundle
                .getBundle(BASE_NAME)
                .getString(key);
    }
}
