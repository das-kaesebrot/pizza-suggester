FROM python:3.8.6-alpine3.12
WORKDIR /srv/musicbot
COPY . ./

# Add build dependencies
RUN apk update && apk add --no-cache --virtual .build-deps \
  py3-pip

# Install pip dependencies
RUN pip3 install --no-cache-dir -r requirements.txt

# Clean up build dependencies
RUN apk del .build-deps

ENV APP_ENV=docker

ENTRYPOINT ["/usr/bin/python3", "kantine-bot.py"]