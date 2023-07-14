# pizza-suggester
## About
`pizza-suggester` (formerly KantineBot) is a telegram bot to suggest you a certain pizza & price for multiple pizzerias when selecting your preferred toppings and diet preferences.

## Using the bot
You may use the bot via Telegram: [@holzofenkantine_bot](https://t.me/holzofenkantine_bot)

## Bulding and running

Clone the repository: `git clone https://github.com/das-kaesebrot/pizza-suggester.git`

Go into the repository folder and create the jar file with gradle: `./gradlew clean bootJar`

Then, execute the jar file with the specified profile:
`java -Dspring.profiles.active=prod -jar build/libs/pizza-suggester-x.x.x.xxx.jar`

## Docker
### Pulling the Docker image

Docker hub: https://hub.docker.com/r/daskaesebrot/pizza-suggester

`docker pull daskaesebrot/pizza-suggester`

### Running via docker-compose
Using a `docker-compose.yml`, define your service and set the specified environment variables.

```yaml
version: "2"

services:
  pizza-suggester:
    image: daskaesebrot/pizza-suggester
    restart: always

    # we need to listen at a port since that is a webhook based bot
    ports:
      - 8080:8080
    
    # adjust values and database settings accordingly
    environment:
      - TELEGRAMBOT_BOTUSERNAME=my_bot_handle
      - TELEGRAMBOT_BOTTOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxx
      - TELEGRAMBOT_WEBHOOKBASEURL=https://example.com
      # database config, adjust accordingly
      - SPRING_DATASOURCE_URL=jdbc:mariadb://your-host:3306/pizza_suggester_db
      - SPRING_DATASOURCE_USERNAME=pizza_suggester
      - SPRING_DATASOURCE_PASSWORD=pizza_suggester
      - SPRING_DATASOURCE_DRIVER-CLASS-NAME=org.mariadb.jdbc.Driver
      - SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MariaDB103Dialect
```

## Configuration
Edit the override file `override.properties` or set the specified environment variables.

### General configuration

In the .properties format:
```properties
## REQUIRED ##
# insert your bot username here
telegrambot.botUsername=my_bot_handle

# insert your token here
telegrambot.botToken=xxxxxxxxxxxxxxxxxxxxxxxxxxxx

# a full callback URL will be generated from this
telegrambot.webhookBaseUrl=https://example.com

## OPTIONAL ##
# sets the locale file used by the bot
telegrambot.primaryLocale=de
```

or as environment variables:
```sh
## REQUIRED ##
# insert your bot username here
TELEGRAMBOT_BOTUSERNAME=my_bot_handle

# insert your token here
TELEGRAMBOT_BOTTOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxx

# a full callback URL will be generated from this
TELEGRAMBOT_WEBHOOKBASEURL=https://example.com

## OPTIONAL ##
# sets the locale file used by the bot
TELEGRAMBOT_PRIMARYLOCALE=de
```
### Supported database backends
#### SQLite
Configure via a .properties file:
```properties
# database properties - SQLite
spring.datasource.url=jdbc:sqlite:pizza-suggester.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
```

Alternatively, set the according environment variables:
```sh
# database env vars - SQLite
SPRING_DATASOURCE_URL=jdbc:sqlite:pizza-suggester.db
SPRING_DATASOURCE_DRIVER-CLASS-NAME=org.sqlite.JDBC
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.community.dialect.SQLiteDialect
```
#### MariaDB
Configure via a .properties file:
```properties
# database properties - MariaDB
spring.datasource.url=jdbc:mariadb://your-host:3306/pizza_suggester_db
spring.datasource.username=pizza_suggester
spring.datasource.password=pizza_suggester
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MariaDB103Dialect
```

Alternatively, set the according environment variables:
```sh
# database env vars - MariaDB
SPRING_DATASOURCE_URL=jdbc:mariadb://your-host:3306/pizza_suggester_db
SPRING_DATASOURCE_USERNAME=pizza_suggester
SPRING_DATASOURCE_PASSWORD=pizza_suggester
SPRING_DATASOURCE_DRIVER-CLASS-NAME=org.mariadb.jdbc.Driver
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MariaDB103Dialect
```

#### PostgreSQL
Configure via a .properties file:
```properties
# database properties - PostgreSQL
spring.datasource.url=jdbc:postgresql://your-host:5432/pizza_suggester_db
spring.datasource.username=pizza_suggester
spring.datasource.password=pizza_suggester
```

Alternatively, set the according environment variables:
```sh
# database env vars - PostgreSQL
SPRING_DATASOURCE_URL=jdbc:postgresql://your-host:5432/pizza_suggester_db
SPRING_DATASOURCE_USERNAME=pizza_suggester
SPRING_DATASOURCE_PASSWORD=pizza_suggester
```

## Open Source License Attribution

This application uses Open Source components. You can find the source code of their open source projects along with license information below. We acknowledge and are grateful to these developers for their contributions to open source.

### [Spring Boot](https://github.com/jmrozanec/cron-utils)
- Copyright (c) 2002 [Spring](https://github.com/spring-projects) and contributors
- [Apache License 2.0](https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt)

### [Git-Version Gradle Plugin](https://github.com/palantir/gradle-git-version)
- Copyright (c) 2015 [Palantir Technologies](https://github.com/palantir) and contributors
- [Apache License 2.0](https://github.com/palantir/gradle-git-version/blob/develop/LICENSE)

### [Telegram Bot Java Library](https://github.com/rubenlagus/TelegramBots)
- Copyright (c) 2016 [Ruben Bermudez](https://github.com/rubenlagus) and contributors
- [MIT License](https://github.com/rubenlagus/TelegramBots/blob/master/LICENSE)

### [SQLite JDBC Driver](https://github.com/xerial/sqlite-jdbc)
- Copyright (c) 2007 [Taro L. Saito](https://github.com/xerial/sqlite-jdbc) and contributors
- [Apache License 2.0](https://github.com/xerial/sqlite-jdbc/blob/master/LICENSE)

### [Apache Commons Lang](https://github.com/apache/commons-lang)
- Copyright (c) 2002 [The Apache Software Foundation](https://github.com/apache) and contributors
- [Apache License 2.0](https://github.com/apache/commons-lang/blob/master/LICENSE.txt)

### [Apache Commons CSV](https://github.com/apache/commons-csv)
- Copyright (c) 2014 [The Apache Software Foundation](https://github.com/apache) and contributors
- [Apache License 2.0](https://github.com/apache/commons-csv/blob/master/LICENSE.txt)

### [MariaDB Connector/J](https://github.com/mariadb-corporation/mariadb-connector-j)
- Copyright (c) 2012 [MariaDB](https://github.com/mariadb-corporation) and contributors
- [GNU Lesser General Public License v2.1](https://github.com/mariadb-corporation/mariadb-connector-j/blob/master/LICENSE)


### [PostgreSQL JDBC Driver](https://github.com/pgjdbc/pgjdbc)
- Copyright (c) [The PostgreSQL Global Development Group](https://www.postgresql.org/) and contributors
- [BSD 2-Clause "Simplified" License](https://github.com/pgjdbc/pgjdbc/blob/master/LICENSE)