package eu.kaesebrot.dev.pizzabot.properties;

import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegrambot")
public class TelegramBotProperties {
    @NotBlank
    private String botUsername;
    @NotBlank
    private String botToken;
    @NotBlank
    private String webhookBaseUrl;
    private String webhookPath = "";
    private String primaryLocale;
    private String[] supportedLocales;

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getWebhookBaseUrl() {
        return webhookBaseUrl;
    }

    public void setWebhookBaseUrl(String webhookBaseUrl) {
        this.webhookBaseUrl = StringUtils.stripEnd(webhookBaseUrl, "/");
    }

    public String getWebhookPath() {
        return webhookPath;
    }

    public void setWebhookPath(String webhookPath) {
        this.webhookPath = StringUtils.stripStart(StringUtils.stripEnd(webhookPath, "/"), "/");;
    }

    public String getFullWebhookUrl() {
        if (eu.kaesebrot.dev.pizzabot.utils.StringUtils.isNullOrEmpty(webhookPath))
            return webhookBaseUrl;

        return String.format("%s/%s", webhookBaseUrl, webhookPath);
    }

    public String getPrimaryLocale() {
        return primaryLocale;
    }

    public void setPrimaryLocale(String primaryLocale) {
        this.primaryLocale = primaryLocale;
    }

    public String[] getSupportedLocales() {
        return supportedLocales;
    }

    public void setSupportedLocales(String[] supportedLocales) {
        this.supportedLocales = supportedLocales;
    }
}
