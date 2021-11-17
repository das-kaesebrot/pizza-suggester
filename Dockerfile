FROM python:3.8.6-alpine3.12
WORKDIR /srv/kantinebot
COPY . ./

# Add build dependencies
RUN apk update && apk add --no-cache --virtual .build-deps \
  py3-pip

# Install pip dependencies
RUN pip3 install --no-cache-dir -r src/requirements

RUN addgroup -S kantinebot \
  && adduser -S kantinebot -G kantinebot -H \
  && chown -R kantinebot:kantinebot ./

# Clean up build dependencies
RUN apk del .build-deps

ENV APP_ENV=docker

STOPSIGNAL SIGINT
USER kantinebot
ENTRYPOINT ["python3", "kantine-bot.py"]