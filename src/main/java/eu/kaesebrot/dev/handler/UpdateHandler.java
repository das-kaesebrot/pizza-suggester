package eu.kaesebrot.dev.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class UpdateHandler implements IUpdateHandler {
    Logger logger = LoggerFactory.getLogger(UpdateHandler.class);

    public enum BotCommand {
        PIZZA,
        RANDOM,
        HELP,
        ABOUT,
        ADMIN,
        CARLOS,
        UNKNOWN
        ;
    }

    @Override
    public BotApiMethod handleUpdate(Update update) {
        logger.debug("Handling update: " + update.toString());

        var message = update.getMessage();
        var callbackQuery = update.getCallbackQuery();

        var reply = new SendMessage();
        reply.setChatId(message.getChatId());
        reply.setText("Unknown operation, use /help for help");

        if (callbackQuery != null) {
            logger.debug("Update type: callbackQuery");
            // TODO handle callback
        }

        if (update.getMessage() != null) {
            var messageText = message.getText();

            if (messageText.length() > 1 && messageText.charAt(0) == '/') {
                logger.debug("Update type: command");
                messageText = messageText.substring(1, messageText.length());

                var command = mapStringToBotCommand(messageText);
                logger.debug("Mapped command string '{}' to enum type '{}'", messageText, command);

                switch (command) {
                    case PIZZA:
                        break;

                    case RANDOM:
                        break;

                    case HELP:
                        break;

                    case CARLOS:
                        break;
                }

            }
        }

        return reply;
    }

    private BotCommand mapStringToBotCommand(String str) {
        switch (str) {
            case "pizza":
                return BotCommand.PIZZA;

            case "zufall":
                return BotCommand.RANDOM;

            case "start":
            case "help":
                return BotCommand.HELP;

            case "carlos":
                return BotCommand.CARLOS;

            default:
                return BotCommand.UNKNOWN;
        }
    }
}
