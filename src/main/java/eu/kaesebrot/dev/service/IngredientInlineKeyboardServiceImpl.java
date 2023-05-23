package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.model.Venue;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Service
public class IngredientInlineKeyboardServiceImpl implements IngredientInlineKeyboardService {
    private static final int maxIngredientKeyboardRows = 3;
    private static final int maxIngredientKeyboardColumns = 2;
    private final VenueRepository venueRepository;
    private final LocalizationService localizationService;
    private HashMap<Long, List<List<List<InlineKeyboardButton>>>> venueInlineKeyboards;
    private HashMap<Long, List<String>> venueIngredients;

    public IngredientInlineKeyboardServiceImpl(VenueRepository venueRepository, LocalizationService localizationService) {
        this.venueRepository = venueRepository;
        this.localizationService = localizationService;
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

        int pagesNeeded = ingredientList.size() / (maxIngredientKeyboardColumns * maxIngredientKeyboardRows);

        if (ingredientList.size() % (maxIngredientKeyboardColumns * maxIngredientKeyboardRows) != 0) {
            pagesNeeded++;
        }

        for (int page = 0; page < pagesNeeded; page++) {
            var pageRows = new ArrayList<List<InlineKeyboardButton>>();

            for (int rowCounter = 0; rowCounter < maxIngredientKeyboardRows; rowCounter++) {
                var row = new ArrayList<InlineKeyboardButton>();

                for (int columnCounter = 0; columnCounter < maxIngredientKeyboardColumns; columnCounter++) {
                    var ingredientIndex = getIngredientIndexAtPageColumnAndRow(page, columnCounter, rowCounter);
                    var button = new InlineKeyboardButton(ingredientList.get(ingredientIndex));
                    button.setCallbackData(String.valueOf(ingredientIndex));

                    row.add(button);
                }

                pageRows.add(row);
            }

            pageRows.add(getLastRow(page));

            listOfPagesInVenue.add(pageRows);
        }

        venueInlineKeyboards.put(venueId, listOfPagesInVenue);
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

    private int getIngredientIndexAtPageColumnAndRow(int pageNumber, int column, int row) {
        int pageOffset = maxIngredientKeyboardColumns * maxIngredientKeyboardRows * pageNumber;
        int indexWithOffset = column + row + pageOffset;

        return indexWithOffset;
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

    private List<InlineKeyboardButton> getLastRow(int pageNumber) {
        // make sure the page number is 1-based instead of 0-based
        pageNumber++;

        var buttonLeft = new InlineKeyboardButton("\u2190");
        buttonLeft.setCallbackData("previous");
        var buttonRight = new InlineKeyboardButton("\u2190");
        buttonRight.setCallbackData("next");
        var buttonPageInfo = new InlineKeyboardButton(localizationService.getString("label.page") + " " + pageNumber);
        buttonLeft.setCallbackData("invalid");
        var buttonConfirm = new InlineKeyboardButton("\u2713");
        buttonRight.setCallbackData("confirm");

        return List.of(buttonLeft, buttonRight, buttonPageInfo, buttonConfirm);
    }
}
