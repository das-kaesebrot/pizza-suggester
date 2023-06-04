package eu.kaesebrot.dev.service;

import eu.kaesebrot.dev.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.enums.UserDiet;
import eu.kaesebrot.dev.enums.UserState;
import eu.kaesebrot.dev.model.CachedUser;
import eu.kaesebrot.dev.model.Venue;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserMenuServiceImpl implements UserMenuService {
    private static final String CALLBACK_DIET_PREFIX = CALLBACK_PREFIX + "-diet";
    private static final String CALLBACK_VENUE_PREFIX = CALLBACK_PREFIX + "-venue";
    private static final String DIET_MESSAGES_PREFIX = "label.diet";
    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final LocalizationService localizationService;

    public UserMenuServiceImpl(CachedUserRepository cachedUserRepository, VenueRepository venueRepository, LocalizationService localizationService) {
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.localizationService = localizationService;
    }

    @Override
    public BotApiMethod<?> handleUserMenuCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException
    {
        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        if (query.getData().startsWith(CALLBACK_VENUE_PREFIX))
        {
            var selectedVenue = query.getData();
            selectedVenue = selectedVenue.replace(CALLBACK_VENUE_PREFIX, "");

            long venueId = Long.parseLong(selectedVenue);
            var venue = venueRepository.findById(venueId);

            if (venue.isEmpty())
                throw new RuntimeException("Couldn't find a venue by given id!");

            user.setSelectedVenue(venue.get());
            user.removeState(UserState.SELECTING_VENUE);
            cachedUserRepository.saveAndFlush(user);

            // delete the message the menu inline keyboard was attached to
            bot.execute(new DeleteMessage(query.getFrom().getId().toString(), query.getMessage().getMessageId()));
        }
        else if (query.getData().startsWith(CALLBACK_DIET_PREFIX))
        {
            var selectedDiet = query.getData();
            selectedDiet = selectedDiet.replace(CALLBACK_DIET_PREFIX, "");

            user.setUserDiet(UserDiet.valueOf(selectedDiet.toUpperCase()));
            user.removeState(UserState.SELECTING_DIET);
            cachedUserRepository.saveAndFlush(user);

            // delete the message the menu inline keyboard was attached to
            bot.execute(new DeleteMessage(query.getFrom().getId().toString(), query.getMessage().getMessageId()));
            bot.execute(new SendMessage(query.getFrom().getId().toString(), String.format("%s: %s", localizationService.getString("select.dietsuccess"), localizationService.getString(DIET_MESSAGES_PREFIX + "." + selectedDiet.toLowerCase()))));
        }

        return reply;
    }

    @Override
    public InlineKeyboardMarkup getDietSelection(CachedUser user)
    {
        user.addState(UserState.SELECTING_DIET);
        cachedUserRepository.save(user);

        return getDietSelection();
    }

    private InlineKeyboardMarkup getDietSelection() {
        var dietButtonRows = new ArrayList<List<InlineKeyboardButton>>();
        var keyboard = new InlineKeyboardMarkup();

        for (UserDiet diet : UserDiet.values())
        {
            var button = new InlineKeyboardButton(localizationService.getString(DIET_MESSAGES_PREFIX + "." + diet.name().toLowerCase()));
            button.setCallbackData(CALLBACK_DIET_PREFIX + "-" + diet.name().toLowerCase());
            dietButtonRows.add(List.of(button));
        }

        keyboard.setKeyboard(dietButtonRows);

        return keyboard;
    }

    @Override
    public InlineKeyboardMarkup getVenueSelection(CachedUser user)
    {
        user.addState(UserState.SELECTING_VENUE);
        cachedUserRepository.save(user);

        return getVenueSelection();
    }

    private InlineKeyboardMarkup getVenueSelection() {
        var venueButtonRows = new ArrayList<List<InlineKeyboardButton>>();
        var keyboard = new InlineKeyboardMarkup();

        for (Venue venue : venueRepository.findAll())
        {
            var button = new InlineKeyboardButton(venue.getName());
            button.setCallbackData(CALLBACK_VENUE_PREFIX + "-" + venue.getId());
            venueButtonRows.add(List.of(button));
        }

        keyboard.setKeyboard(venueButtonRows);

        return keyboard;
    }
}
