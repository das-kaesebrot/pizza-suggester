FROM python:3.9.9-alpine3.14
WORKDIR /srv/kantinebot
COPY . ./

# Add build dependencies
RUN apk update && apk add --no-cache --virtual .build-deps \
  py3-pip

# Install pip dependencies
RUN pip3 install --no-cache-dir -r requirements.txt

RUN addgroup -S kantinebot \
  && adduser -S kantinebot -G kantinebot -H \
  && chown -R kantinebot:kantinebot ./

# Clean up build dependencies
RUN apk del .build-deps

ENV APP_ENV=docker

STOPSIGNAL SIGINT
USER kantinebot
CMD ["gunicorn", "-w", "1", "-b", "0.0.0.0:8000", "kantine-bot:app"]