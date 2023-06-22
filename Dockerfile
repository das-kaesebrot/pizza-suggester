FROM openjdk:17-jdk-slim

ARG BOT_UNIX_USER=pizzabot

# Create user and group to run as
RUN addgroup --system ${BOT_UNIX_USER} && adduser --system --group ${BOT_UNIX_USER}

ARG SPRING_FOLDER=/var/opt/pizza-suggester

# Create settings and logs folder
RUN mkdir -pv ${SPRING_FOLDER}/logs && \
    mkdir -pv ${SPRING_FOLDER}/config

# Set path for log file
ENV LOGGING_FILE_NAME=${SPRING_FOLDER}/logs/pizzabot.log

# Copy over override.properties file to container (ENV variables take precedence)
ENV SPRING_CONFIG_IMPORT=optional:file:${SPRING_FOLDER}/config/override.properties
COPY src/main/resources/application.properties.sample ${SPRING_FOLDER}/config/override.properties

ENV SPRING_PROFILES_ACTIVE=prod

# Set proper perms
RUN chown -R ${BOT_UNIX_USER}:${BOT_UNIX_USER} ${SPRING_FOLDER}

# Set run user and group
USER ${BOT_UNIX_USER}:${BOT_UNIX_USER}

# Copy over compiled jar

ARG JAR_FILE
ENV JAR_PATH=build/libs/${JAR_FILE}

WORKDIR ${SPRING_FOLDER}
COPY ${JAR_PATH} app.jar

RUN if [ ! -f "app.jar" ]; then exit 1; fi

CMD ["java","-jar","app.jar"]
