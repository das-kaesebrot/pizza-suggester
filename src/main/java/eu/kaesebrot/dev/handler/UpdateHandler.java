package eu.kaesebrot.dev.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Service
public class UpdateHandler implements IUpdateHandler {
    Logger logger = LoggerFactory.getLogger(UpdateHandler.class);

    // TODO replace with actual data
    private List<String> ingredientListDummy;

    private List<List<List<InlineKeyboardButton>>> inlineKeyboardPages;
    private final int maxIngredientKeyboardRows = 3;
    private final int maxIngredientKeyboardColumns = 2;

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
                messageText = messageText.substring(1);

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

    private InlineKeyboardMarkup getInitialKeyboard() {
        var keyboard = new InlineKeyboardMarkup();

        getKeyboardForPage(null);

        return keyboard;
    }

    private InlineKeyboardMarkup getKeyboardForPage(Optional<Integer> pageNumber) {
        return new InlineKeyboardMarkup(getKeyboardButtonsForPage(pageNumber));
    }

    private List<List<InlineKeyboardButton>> getKeyboardButtonsForPage(Optional<Integer> pageNumber) {
        if (!inlineKeyboardPages.isEmpty()) {
            regenerateInlineKeyboardPageCache();
        }

        return inlineKeyboardPages.get(pageNumber.orElse(0));
    }

    private void regenerateInlineKeyboardPageCache() {
        // clear cache whenever this method is called
        inlineKeyboardPages = new ArrayList<>();

        int pagesNeeded = ingredientListDummy.size() / (maxIngredientKeyboardColumns * maxIngredientKeyboardRows);

        if (ingredientListDummy.size() % (maxIngredientKeyboardColumns * maxIngredientKeyboardRows) != 0) {
            pagesNeeded++;
        }

        for (int page = 0; page < pagesNeeded; page++) {
            var pageRows = new ArrayList<List<InlineKeyboardButton>>();

            for (int rowCounter = 0; rowCounter < maxIngredientKeyboardRows; rowCounter++) {
                var row = new ArrayList<InlineKeyboardButton>();

                for (int columnCounter = 0; columnCounter < maxIngredientKeyboardColumns; columnCounter++) {
                    var ingredientIndex = getIngredientIndexAtPageColumnAndRow(page, columnCounter, rowCounter);
                    var button = new InlineKeyboardButton(ingredientListDummy.get(ingredientIndex));
                    button.setCallbackData(String.valueOf(ingredientIndex));

                    row.add(button);
                }

                pageRows.add(row);
            }

            pageRows.add(getLastRow(page));

            inlineKeyboardPages.add(pageRows);
        }
    }

    private List<InlineKeyboardButton> getLastRow(int pageNumber) {
        // make sure the page number is 1-based instead of 0-based
        pageNumber++;

        var buttonLeft = new InlineKeyboardButton("\u2190");
        buttonLeft.setCallbackData("previous");
        var buttonRight = new InlineKeyboardButton("\u2190");
        buttonRight.setCallbackData("next");
        var buttonPageInfo = new InlineKeyboardButton("Seite " + pageNumber);
        buttonLeft.setCallbackData("invalid");
        var buttonConfirm = new InlineKeyboardButton("\u2713");
        buttonRight.setCallbackData("confirm");

        return List.of(buttonLeft, buttonRight, buttonPageInfo, buttonConfirm);
    }

    private int getIngredientIndexAtPageColumnAndRow(int pageNumber, int column, int row) {
        int pageOffset = maxIngredientKeyboardColumns * maxIngredientKeyboardRows * pageNumber;
        int indexWithOffset = column + row + pageOffset;

        return indexWithOffset;
    }
}
