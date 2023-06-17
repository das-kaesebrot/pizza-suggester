package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.classes.IngredientButtonList;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import eu.kaesebrot.dev.pizzabot.service.InlineKeyboardService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.service.PizzaService;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Service
public class PizzaMenuServiceImpl implements PizzaMenuService {
    private final VenueRepository venueRepository;
    private final InlineKeyboardService inlineKeyboardService;
    private final PizzaService pizzaService;
    private final LocalizationService localizationService;
    private HashMap<Long, IngredientButtonList> venueIngredientButtons = new HashMap<>();

    public PizzaMenuServiceImpl(VenueRepository venueRepository, LocalizationService localizationService, InlineKeyboardService inlineKeyboardService, PizzaService pizzaService, LocalizationService localizationService1) {
        this.venueRepository = venueRepository;
        this.inlineKeyboardService = inlineKeyboardService;
        this.pizzaService = pizzaService;
        this.localizationService = localizationService1;
    }

    @Override
    public InlineKeyboardMarkup getKeyboardForPage(Long venueId, Optional<Integer> pageNumber) {
        regenerateInlineKeyboardPageCache();

        return new InlineKeyboardMarkup(getKeyboardButtonsForPage(venueId, pageNumber));
    }

    @Override
    public InlineKeyboardMarkup getInitialKeyboard(Long venueId) {
        var keyboard = new InlineKeyboardMarkup();

        return getKeyboardForPage(venueId, null);
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException {

        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        var sanitizedData = stripCallbackPrefix(query.getData());
        int pageNumber = getPageNumberFromCallbackData(sanitizedData);
        sanitizedData = stripPageNumberFromCallbackData(sanitizedData);

        switch (sanitizedData) {

            case InlineKeyboardService.CALLBACK_NAVIGATION_CLOSE:
                reply.setText(localizationService.getString("admin.closed"));
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
                reply.setText(localizationService.getString("admin.pagepress"));
                break;

            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
                var editMessage = new EditMessageReplyMarkup();
                editMessage.setChatId(user.getChatId().toString());
                editMessage.setMessageId(query.getMessage().getMessageId());
                editMessage.setReplyMarkup(getKeyboardForPage(user.getSelectedVenue().getId(), Optional.of(pageNumber)));

                bot.execute(editMessage);
                break;

            default:
                throw new IllegalArgumentException(String.format("Unknown callback data for admin menu! Given data: %s, CallbackQuery: %s", query.getData(), query));
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        switch (stripPageNumberFromCallbackData(stripCallbackPrefix(query.getData()))) {
            case InlineKeyboardService.CALLBACK_NAVIGATION_FORWARD:
            case InlineKeyboardService.CALLBACK_NAVIGATION_BACK:
            case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
            case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
                return false;

            default:
                return true;
        }
    }

    private List<List<InlineKeyboardButton>> getKeyboardButtonsForPage(Long venueId, Optional<Integer> pageNumber) {
        regenerateInlineKeyboardPageCache();

        return venueIngredientButtons.get(venueId).ingredientButtons().get(pageNumber.orElse(0));
    }

    private void regenerateInlineKeyboardPageCache() {
        for (var venue : venueRepository.findAll()) {
            var ingredientList = pizzaService.getVenueIngredientList(venue);

            if ((venueIngredientButtons.containsKey(venue.getId()) || venueIngredientButtons.get(venue.getId()) != null)
                    && venueIngredientButtons.get(venue.getId()).createdAt().after(venue.getModifiedAt()))
                continue;

            var listOfIngredientButtons = new ArrayList<InlineKeyboardButton>();

            for (int index = 0; index < ingredientList.ingredients().size(); index++) {
                var button = new InlineKeyboardButton(ingredientList.ingredients().get(index));
                button.setCallbackData(String.format("%s-%d", CALLBACK_PREFIX, index));

                listOfIngredientButtons.add(button);
            }

            venueIngredientButtons.put(venue.getId(),
                    new IngredientButtonList(
                    inlineKeyboardService.getPagedInlineKeyboardButtons(
                            listOfIngredientButtons, maxIngredientKeyboardColumns, maxIngredientKeyboardRows,
                            true, CALLBACK_PREFIX, true, false),
                            Timestamp.from(Instant.now())));
        }
    }
    private String stripCallbackPrefix(String data) {
        return StringUtils.stripCallbackPrefix(CALLBACK_PREFIX, data);
    }

    private String prependCallbackPrefix(String data) {
        return StringUtils.prependCallbackPrefix(CALLBACK_PREFIX, data);
    }

    private int getPageNumberFromCallbackData(String data) {
        if (data.startsWith(InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE))
            return Integer.parseInt(data.replace(String.format("%s--", InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE), ""));

        return 0;
    }

    private String stripPageNumberFromCallbackData(String data) {
        return data.replaceAll("--\\d*$", "");
    }
}
