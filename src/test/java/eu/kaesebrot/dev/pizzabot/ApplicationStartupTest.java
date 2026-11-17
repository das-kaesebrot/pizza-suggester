package eu.kaesebrot.dev.pizzabot;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.repository.AdminKeyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.config.import=",
        "telegrambot.botToken=test-token",
        "spring.datasource.url=jdbc:sqlite::memory:",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.file.name=${java.io.tmpdir}/pizza-suggester-test.log"
})
class ApplicationStartupTest {
    @MockitoBean
    PizzaSuggesterBot webhookClient;

    @Autowired
    AdminKeyRepository adminKeys;

    @Test
    void startsWithSQLite() {
        assertThat(adminKeys.count()).isZero();
    }
}
