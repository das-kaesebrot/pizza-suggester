package eu.kaesebrot.dev.service.menu;

import eu.kaesebrot.dev.model.Venue;
import eu.kaesebrot.dev.repository.VenueRepository;
import eu.kaesebrot.dev.service.InlineKeyboardService;
import eu.kaesebrot.dev.service.LocalizationService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Service
public class IngredientMenuServiceImpl implements IngredientMenuService {
    private static final int maxIngredientKeyboardRows = 3;
    private static final int maxIngredientKeyboardColumns = 2;
    private static final String CALLBACK_PREFIX = "ingredients";
    private final VenueRepository venueRepository;
    private final InlineKeyboardService inlineKeyboardService;
    private HashMap<Long, List<List<List<InlineKeyboardButton>>>> venueInlineKeyboards;
    private HashMap<Long, List<String>> venueIngredients;

    public IngredientMenuServiceImpl(VenueRepository venueRepository, LocalizationService localizationService, InlineKeyboardService inlineKeyboardService) {
        this.venueRepository = venueRepository;
        this.inlineKeyboardService = inlineKeyboardService;
    }

    @Override
    public void regenerateInlineKeyboardPageCache() {
        for (var venue : venueRepository.findAll()) {
            regenerateInlineKeyboardPageCache(venue.getId());
        }
    }

    @Override
    public void regenerateInlineKeyboardPageCache(Long venueId) {
        // clear cache whenever this method is called
        var listOfPagesInVenue = new ArrayList<List<List<InlineKeyboardButton>>>();
        var ingredientList = getVenueIngredients(venueId);

        var listOfIngredientButtons = new ArrayList<InlineKeyboardButton>();

        for (int index = 0; index < ingredientList.size(); index++) {
            var button = new InlineKeyboardButton(ingredientList.get(index));
            button.setCallbackData(String.format("%s-%d", CALLBACK_PREFIX, index));

            listOfIngredientButtons.add(button);
        }

        venueInlineKeyboards.put(venueId,
                inlineKeyboardService.getPagedInlineKeyboardButtons(
                        listOfIngredientButtons, maxIngredientKeyboardColumns, maxIngredientKeyboardRows,
                        true, CALLBACK_PREFIX, true));
    }

    @Override
    public InlineKeyboardMarkup getInitialKeyboard(Long venueId) {
        var keyboard = new InlineKeyboardMarkup();

        if (!venueInlineKeyboards.containsKey(venueId)) {
            regenerateInlineKeyboardPageCache(venueId);
        }

        return getKeyboardForPage(venueId, null);
    }

    @Override
    public InlineKeyboardMarkup getKeyboardForPage(Long venueId, Optional<Integer> pageNumber) {
        return new InlineKeyboardMarkup(getKeyboardButtonsForPage(venueId, pageNumber));
    }

    private List<List<InlineKeyboardButton>> getKeyboardButtonsForPage(Long venueId, Optional<Integer> pageNumber) {
        if (venueInlineKeyboards.isEmpty()) {
            regenerateInlineKeyboardPageCache();
        }

        if (!venueInlineKeyboards.containsKey(venueId)) {
            regenerateInlineKeyboardPageCache(venueId);
        }

        return venueInlineKeyboards.get(venueId).get(pageNumber.orElse(0));
    }

    private List<String> getVenueIngredients(Long venueId) {
        var venue = venueRepository.findById(venueId).get();

        return getVenueIngredients(venue);
    }

    private List<String> getVenueIngredients(Venue venue) {

        if (!venueIngredients.containsKey(venue.getId())) {
            // TODO populate here...
            var ingredientListDummy = new ArrayList<String>();
        }

        return venueIngredients.get(venue.getId());
    }
}
