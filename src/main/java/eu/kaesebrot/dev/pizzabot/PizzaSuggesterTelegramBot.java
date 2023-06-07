package eu.kaesebrot.dev.pizzabot;

import eu.kaesebrot.dev.pizzabot.properties.TelegramBotProperties;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TelegramBotProperties.class)
public class PizzaSuggesterTelegramBot {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PizzaSuggesterTelegramBot.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}