FROM gradle:9-jdk21@sha256:9795f8c9009a7de6adea522b907c9aadc38fe7c313874112c2b696c8e70b0303 AS build

ARG OUT_DIR=/srv/final

COPY . .
RUN gradle clean bootJar && \
    mkdir -pv ${OUT_DIR} && \
    mv -v build/libs/*.jar ${OUT_DIR}/app.jar

FROM eclipse-temurin:25.0.3_9-jre@sha256:681c543d6f36c50f45e9b5226930a46203dcfa351d3670e9d0bdf0dabae53539 AS app

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
