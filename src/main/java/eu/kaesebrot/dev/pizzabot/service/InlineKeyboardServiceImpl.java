package eu.kaesebrot.dev.pizzabot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class InlineKeyboardServiceImpl implements InlineKeyboardService {
    private final LocalizationService localizationService;

    public InlineKeyboardServiceImpl(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @Override
    public List<List<List<InlineKeyboardButton>>> getPagedInlineKeyboardButtons(List<InlineKeyboardButton> buttons, boolean withNavigationFooter, String navigationFooterCallbackPrefix) {
        return getPagedInlineKeyboardButtons(buttons, DEFAULT_COLUMNS, DEFAULT_ROWS, withNavigationFooter, navigationFooterCallbackPrefix, false, false);
    }

    @Override
    public List<List<List<InlineKeyboardButton>>> getPagedInlineKeyboardButtons
            (
                List<InlineKeyboardButton> buttons,
                long columns,
                long rows,
                boolean withNavigationFooter,
                String navigationFooterCallbackPrefix,
                boolean withCheckmark,
                boolean withClose
            )
    {
        var listOfPages = new ArrayList<List<List<InlineKeyboardButton>>>();

        if (buttons == null || buttons.isEmpty()) {
            throw new IllegalArgumentException("List of buttons can't be empty");
        }

        long pagesNeeded = buttons.size() / (columns * rows);

        if (buttons.size() % (columns * rows) != 0) {
            pagesNeeded++;
        }

        for (long page = 0; page < pagesNeeded; page++) {
            var pageRows = new ArrayList<List<InlineKeyboardButton>>();

            for (long rowCounter = 0; rowCounter < rows; rowCounter++) {
                var row = new ArrayList<InlineKeyboardButton>();

                for (long columnCounter = 0; columnCounter < columns; columnCounter++) {
                    long pageOffset = columns * rows * page;
                    long indexWithOffset = columnCounter + rowCounter + pageOffset;

                    row.add(buttons.get((int) indexWithOffset));
                }

                pageRows.add(row);
            }

            if (withNavigationFooter)
                pageRows.add(getNavigationFooter(page, pagesNeeded, navigationFooterCallbackPrefix, withCheckmark, withClose));

            listOfPages.add(pageRows);
        }

        return listOfPages;
    }

    @Override
    public List<InlineKeyboardButton> getNavigationFooter(Long zeroBasedPageIndex, Long totalAmountOfPages, String callbackPrefix, boolean withCheckmark, boolean withClose) {
        var prefix = "";
        ArrayList<InlineKeyboardButton> additionalButtons = new ArrayList<>();

        if (!callbackPrefix.isEmpty() || !callbackPrefix.isBlank())
            prefix = String.format("%s-", callbackPrefix);

        var buttonBack = new InlineKeyboardButton(localizationService.getString("label.back"));
        buttonBack.setCallbackData(String.format("%s%s", prefix, CALLBACK_NAVIGATION_BACK));

        var buttonForward = new InlineKeyboardButton(localizationService.getString("label.forward"));
        buttonForward.setCallbackData(String.format("%s%s", prefix, CALLBACK_NAVIGATION_FORWARD));

        var buttonPages = new InlineKeyboardButton(String.format("%s %d/%d", localizationService.getString("label.page"), zeroBasedPageIndex + 1, totalAmountOfPages));
        buttonPages.setCallbackData(String.format("%s%s", prefix, CALLBACK_NAVIGATION_PAGE));

        if (withCheckmark) {
            var buttonCheckmark = new InlineKeyboardButton(localizationService.getString("label.checkmark"));
            buttonCheckmark.setCallbackData(String.format("%s%s", prefix, CALLBACK_NAVIGATION_CONFIRM));

            additionalButtons.add(buttonCheckmark);
        }

        if (withClose) {
            var buttonClose = new InlineKeyboardButton(localizationService.getString("label.close"));
            buttonClose.setCallbackData(String.format("%s%s", prefix, CALLBACK_NAVIGATION_CLOSE));

            additionalButtons.add(buttonClose);
        }

        return Stream.concat(Stream.of(buttonBack, buttonForward, buttonPages), additionalButtons.stream()).toList();
    }
}
