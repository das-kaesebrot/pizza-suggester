FROM gradle:9-jdk21@sha256:105f4423a690be340bdef02ea2efd80060c3e714e4afba9ea41e7a38ba484e01 AS build

ARG OUT_DIR=/srv/final

COPY . .
RUN gradle clean bootJar && \
    mkdir -pv ${OUT_DIR} && \
    mv -v build/libs/*.jar ${OUT_DIR}/app.jar

FROM eclipse-temurin:21.0.8_9-jre@sha256:66bb900643426ad01996d25bada7d56751913f9cec3b827fcb715d2ec9a0fbfc AS app

ARG BOT_UNIX_USER=pizzabot

# Create user and group to run as
RUN useradd --system ${BOT_UNIX_USER}

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
ARG JAR_PATH=/srv/final/app.jar

WORKDIR ${SPRING_FOLDER}
COPY --from=build ${JAR_PATH} app.jar

RUN if [ ! -f "app.jar" ]; then exit 1; fi

CMD ["java","-jar","app.jar"]
