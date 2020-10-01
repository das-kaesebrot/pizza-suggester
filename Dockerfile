FROM python:3.8.6-alpine3.12
WORKDIR /srv/musicbot
COPY . ./

# Add build dependencies
RUN apk update && apk add --no-cache --virtual .build-deps \
  py3-pip

# Install pip dependencies
RUN pip3 install --no-cache-dir -r src/requirements

# Clean up build dependencies
RUN apk del .build-deps

ENV APP_ENV=docker

# RUN mkdir /var/log/musicbot \
  # && ln -sf /dev/stdout /var/log/musicbot/access.log \
	# && ln -sf /dev/stderr /var/log/musicbot/error.log

STOPSIGNAL SIGINT

ENTRYPOINT ["python3", "kantine-bot.py"]