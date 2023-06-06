package eu.kaesebrot.dev.properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegrambot")
public class TelegramBotProperties {
    private String botUsername;
    private String botToken;
    private String webhookBaseUrl;
    private String primaryLocale;

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

    public String getPrimaryLocale() {
        return primaryLocale;
    }

    public void setPrimaryLocale(String primaryLocale) {
        this.primaryLocale = primaryLocale;
    }
}
