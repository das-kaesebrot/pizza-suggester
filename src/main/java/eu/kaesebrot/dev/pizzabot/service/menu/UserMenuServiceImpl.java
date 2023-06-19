package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.exceptions.PendingVenueSelectionException;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Pizza;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import eu.kaesebrot.dev.pizzabot.properties.VersionProperties;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import eu.kaesebrot.dev.pizzabot.service.InlineKeyboardService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.service.PizzaService;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class UserMenuServiceImpl implements UserMenuService {
    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final InlineKeyboardService inlineKeyboardService;
    private final LocalizationService localizationService;
    private final VersionProperties versionProperties;
    private List<List<List<InlineKeyboardButton>>> pagedVenueSelectionMenu;
    private Timestamp lastPagedVenueSelectionUpdate;
    private long lastAmountOfVenuesInRepository = 0;

    public UserMenuServiceImpl(CachedUserRepository cachedUserRepository, VenueRepository venueRepository, InlineKeyboardService inlineKeyboardService, LocalizationService localizationService) {
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localizationService = localizationService;

        versionProperties = new VersionProperties();
    }

    @Override
    public String getCallbackPrefix() {
        return CALLBACK_PREFIX;
    }

    @Override
    public BotApiMethod<?> handleCallback(CachedUser user, CallbackQuery query, PizzaSuggesterBot bot) throws TelegramApiException
    {
        AnswerCallbackQuery reply = new AnswerCallbackQuery(query.getId());

        var sanitizedData = stripCallbackPrefix(query.getData());

        if (sanitizedData.startsWith(CALLBACK_VENUE_PREFIX))
        {
            var selectedVenue = sanitizedData;
            selectedVenue = selectedVenue.replace(String.format("%s-", CALLBACK_VENUE_SELECTION), "");

            long venueId = Long.parseLong(selectedVenue);
            var venue = venueRepository.findById(venueId);

            if (venue.isEmpty())
                throw new RuntimeException("Couldn't find a venue by given id!");

            user.setSelectedVenue(venue.get());
            user.removeState(UserState.SELECTING_VENUE);
            cachedUserRepository.saveAndFlush(user);

            bot.execute(new SendMessage(query.getFrom().getId().toString(),
                    StringUtils.replacePropertiesVariable("venue_name", venue.get().getName(), localizationService.getString("select.venuesuccess"))));
        }
        else if (sanitizedData.startsWith(CALLBACK_DIET_PREFIX))
        {
            var selectedDiet = sanitizedData.replace(String.format("%s-", CALLBACK_DIET_PREFIX), "");

            user.setUserDiet(UserDiet.valueOf(selectedDiet.toUpperCase()));
            user.removeState(UserState.SELECTING_DIET);
            cachedUserRepository.saveAndFlush(user);

            bot.execute(new SendMessage(query.getFrom().getId().toString(),
                    StringUtils.replacePropertiesVariable("diet_name",
                            localizationService.getString(String.format("%s.%s", MESSAGES_LABEL_DIET_PREFIX, selectedDiet.toLowerCase())),
                            localizationService.getString("select.dietsuccess"))));
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        return true;
    }

    @Override
    public SendMessage getDietSelection(CachedUser user) {
        SendMessage dietSelection = new SendMessage();
        dietSelection.setChatId(user.getChatId());
        dietSelection.setText(localizationService.getString("select.diet"));
        dietSelection.setReplyMarkup(getDietSelectionMarkup());

        user.addState(UserState.SELECTING_DIET);
        cachedUserRepository.save(user);

        return dietSelection;
    }

    private InlineKeyboardMarkup getDietSelectionMarkup() {
        var dietButtonRows = new ArrayList<List<InlineKeyboardButton>>();
        var keyboard = new InlineKeyboardMarkup();

        for (UserDiet diet : UserDiet.values())
        {
            var button = new InlineKeyboardButton(localizationService.getString(String.format("%s.%s", MESSAGES_LABEL_DIET_PREFIX, diet.name().toLowerCase())));
            button.setCallbackData(prependCallbackPrefix(String.format("%s-%s", CALLBACK_DIET_PREFIX, diet.name().toLowerCase())));
            dietButtonRows.add(List.of(button));
        }

        keyboard.setKeyboard(dietButtonRows);

        return keyboard;
    }

    @Override
    public SendMessage getVenueSelection(CachedUser user) {
        SendMessage venueSelection = new SendMessage();
        venueSelection.setChatId(user.getChatId());
        venueSelection.setText(localizationService.getString("select.venue"));
        venueSelection.setReplyMarkup(getVenueSelectionMarkup());

        user.addState(UserState.SELECTING_VENUE);
        cachedUserRepository.save(user);

        return venueSelection;
    }

    @Override
    public SendMessage getSetupMessages(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException {
        bot.execute(new SendMessage(user.getChatId().toString(), localizationService.getString("reply.firstrun")));

        if (!venueRepository.findAll().isEmpty()) {
            bot.execute(getVenueSelection(user));
        }

        bot.execute(getDietSelection(user));

        bot.execute(new SendMessage(user.getChatId().toString(), localizationService.getString("select.disclaimer")));

        return getHelpMessage(user);
    }

    @Override
    public SendMessage getHelpMessageWithGreeting(CachedUser user) {
        var msg = new SendMessage(user.getChatId().toString(),
                String.format("%s\n%s", localizationService.getString("reply.greeting"), localizationService.getString("reply.help")));
        msg.setParseMode(ParseMode.MARKDOWNV2);

        return msg;
    }

    @Override
    public SendMessage getHelpMessage(CachedUser user) {
        var msg = new SendMessage(user.getChatId().toString(), localizationService.getString("reply.help"));
        msg.setParseMode(ParseMode.MARKDOWNV2);

        return msg;
    }

    @Override
    public SendMessage getAboutMessage(CachedUser user) {
        var text = localizationService.getString("reply.about");
        text = StringUtils.replacePropertiesVariable("bot_handle", "KantineBot", text);
        text = StringUtils.replacePropertiesVariable("technical_name", "pizza-suggester", text);
        text = StringUtils.replacePropertiesVariable("git_hash", versionProperties.getGitHash(), text);
        text = StringUtils.replacePropertiesVariable("git_branch", versionProperties.getGitBranch(), text);
        text = StringUtils.replacePropertiesVariable("build_date", versionProperties.getBuildDate().toString(), text);

        var msg = new SendMessage(user.getChatId().toString(), text);
        msg.setParseMode(ParseMode.MARKDOWNV2);
        msg.disableWebPagePreview();

        return msg;
    }

    private InlineKeyboardMarkup getVenueSelectionMarkup() {
        var keyboard = new InlineKeyboardMarkup();
        regenerateMenuCaches();

        keyboard.setKeyboard(pagedVenueSelectionMenu.get(0));

        return keyboard;
    }

    private String formatVenueForButton(Venue venue) {
        var venueInfoText = localizationService.getString("venue.info");
        venueInfoText = StringUtils.replacePropertiesVariable("venue_name", venue.getName(), venueInfoText);
        venueInfoText = StringUtils.replacePropertiesVariable("venue_address", venue.getVenueInfo().getAddress(), venueInfoText);

        return venueInfoText;
    }

    private List<InlineKeyboardButton> getVenueButtons() {
        var venueButtons = new ArrayList<InlineKeyboardButton>();

        for (Venue venue : venueRepository.findAll())
        {
            var button = new InlineKeyboardButton(formatVenueForButton(venue));
            button.setCallbackData(prependCallbackPrefix(String.format("%s-%d", CALLBACK_VENUE_SELECTION, venue.getId())));
            venueButtons.add(button);
        }

        return venueButtons;
    }

    private void regenerateMenuCaches() {
        if (pagedVenueSelectionMenu == null ||
                pagedVenueSelectionMenu.isEmpty() ||
                lastPagedVenueSelectionUpdate == null ||
                venueRepository.existsByModifiedAtAfter(lastPagedVenueSelectionUpdate) ||
                venueRepository.existsByVenueInfoModifiedAtAfter(lastPagedVenueSelectionUpdate) ||
                lastAmountOfVenuesInRepository != venueRepository.count()
        ) {
            pagedVenueSelectionMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCloseButton(getVenueButtons(), VENUE_SELECTION_COLUMNS, VENUE_SELECTION_ROWS, CALLBACK_VENUE_PREFIX);
            lastPagedVenueSelectionUpdate = Timestamp.from(Instant.now());
            lastAmountOfVenuesInRepository = venueRepository.count();
        }
    }
}
