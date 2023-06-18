package eu.kaesebrot.dev.pizzabot.service;

import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
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
                boolean withClose,
                boolean withBackButton
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

                    if (indexWithOffset >= buttons.size())
                        break;

                    row.add(buttons.get((int) indexWithOffset));
                }

                pageRows.add(row);
            }

            if (withNavigationFooter)
                pageRows.add(getNavigationFooter(page, pagesNeeded, navigationFooterCallbackPrefix, withCheckmark, withClose, withBackButton));

            listOfPages.add(pageRows);
        }

        return listOfPages;
    }

    @Override
    public List<InlineKeyboardButton> getNavigationFooter(Long zeroBasedPageIndex, Long totalAmountOfPages, String callbackPrefix, boolean withCheckmark, boolean withClose, boolean withBackButton) {
        var prefix = "";
        ArrayList<InlineKeyboardButton> additionalButtons = new ArrayList<>();

        if (!StringUtils.isNullOrEmpty(callbackPrefix))
            prefix = String.format("%s-", callbackPrefix);

        long nextPage = zeroBasedPageIndex + 1;
        if (nextPage >= totalAmountOfPages)
            nextPage = 0;

        long previousPage = zeroBasedPageIndex - 1;
        if (previousPage < 0)
            previousPage = totalAmountOfPages - 1;

        var buttonPreviousPage = new InlineKeyboardButton(localizationService.getString("label.prevpage"));
        buttonPreviousPage.setCallbackData(String.format("%s%s--%d", prefix, CALLBACK_NAVIGATION_GETPAGE, nextPage));

        var buttonNextPage = new InlineKeyboardButton(localizationService.getString("label.nextpage"));
        buttonNextPage.setCallbackData(String.format("%s%s--%d", prefix, CALLBACK_NAVIGATION_GETPAGE, previousPage));

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

        if (withBackButton) {
            var buttonBack = new InlineKeyboardButton(localizationService.getString("label.back"));
            buttonBack.setCallbackData(String.format("%s%s", prefix, CALLBACK_NAVIGATION_BACK));

            additionalButtons.add(buttonBack);
        }

        return Stream.concat(Stream.of(buttonPreviousPage, buttonNextPage, buttonPages), additionalButtons.stream()).toList();
    }
}
