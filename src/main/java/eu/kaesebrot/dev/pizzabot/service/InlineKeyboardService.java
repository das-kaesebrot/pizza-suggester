package eu.kaesebrot.dev.pizzabot.service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

public interface InlineKeyboardService {
    public final Long DEFAULT_COLUMNS = 2L;
    public final Long DEFAULT_ROWS = 3L;
    public final String CALLBACK_NAVIGATION_FORWARD = "fwd";
    public final String CALLBACK_NAVIGATION_BACK = "back";
    public final String CALLBACK_NAVIGATION_PAGE = "invalid";
    public final String CALLBACK_NAVIGATION_CONFIRM = "confirm";
    public final String CALLBACK_NAVIGATION_CLOSE = "close-menu";

    /**
     * Rows and columns use default values {@link #DEFAULT_COLUMNS} and {@link #DEFAULT_ROWS}. A checkmark is not inserted in the navigation.
     * @param buttons The buttons to be used for generating the pages
     * @param withNavigationFooter Whether to add a footer for navigation purposes (back/forward buttons, page number, etc)
     * @param navigationFooterCallbackPrefix Prefix to use for generating navigation buttons. Can be null or empty.
     * @return A list containing all pages with the corresponding data
     */
    List<List<List<InlineKeyboardButton>>> getPagedInlineKeyboardButtons(List<InlineKeyboardButton> buttons, boolean withNavigationFooter, String navigationFooterCallbackPrefix);

    /**
     * @param buttons The buttons to be used for generating the pages
     * @param columns The maximum amount of columns to use for generating a page
     * @param rows The maximum amount of rows (excluding the nav footer) to use for generating a page
     * @param withNavigationFooter Whether to add a footer for navigation purposes (back/forward buttons, page number, etc)
     * @param navigationFooterCallbackPrefix Prefix to use for generating navigation buttons. Can be null or empty.
     * @param navigationWithCheckmarkButton Whether to add a checkmark button to the navigation footer.
     * @param navigationWithCloseButton Whether to add a Close button to the navigation footer.
     * @return A list containing all pages with the corresponding data
     */
    List<List<List<InlineKeyboardButton>>> getPagedInlineKeyboardButtons(List<InlineKeyboardButton> buttons, long columns, long rows, boolean withNavigationFooter, String navigationFooterCallbackPrefix, boolean navigationWithCheckmarkButton, boolean navigationWithCloseButton);


    /**
     * @param zeroBasedPageIndex Current page to display, zero-based
     * @param totalAmountOfPages Total amount of pages
     * @param callbackPrefix Prefix to use for callback data. Can be null or empty.
     * @param withCheckmark Whether to add a checkmark button to the navigation footer.
     * @param withClose Whether to add a Close button to the navigation footer.
     * @return A list of keyboard buttons to be used in an inline keyboard.
     */
    List<InlineKeyboardButton> getNavigationFooter(Long zeroBasedPageIndex, Long totalAmountOfPages, String callbackPrefix, boolean withCheckmark, boolean withClose);
}
