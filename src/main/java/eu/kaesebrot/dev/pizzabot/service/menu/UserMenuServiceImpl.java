package eu.kaesebrot.dev.pizzabot.service.menu;

import eu.kaesebrot.dev.pizzabot.PizzaSuggesterTelegramBotMain;
import eu.kaesebrot.dev.pizzabot.bot.PizzaSuggesterBot;
import eu.kaesebrot.dev.pizzabot.enums.UserDiet;
import eu.kaesebrot.dev.pizzabot.enums.UserState;
import eu.kaesebrot.dev.pizzabot.exceptions.PendingVenueSelectionException;
import eu.kaesebrot.dev.pizzabot.model.CachedUser;
import eu.kaesebrot.dev.pizzabot.model.Venue;
import eu.kaesebrot.dev.pizzabot.properties.VersionProperties;
import eu.kaesebrot.dev.pizzabot.repository.CachedUserRepository;
import eu.kaesebrot.dev.pizzabot.repository.VenueRepository;
import eu.kaesebrot.dev.pizzabot.service.InlineKeyboardService;
import eu.kaesebrot.dev.pizzabot.service.LocalizationService;
import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinAllChatMessages;
import org.telegram.telegrambots.meta.api.methods.send.SendContact;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVenue;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class UserMenuServiceImpl implements UserMenuService {
    private final CachedUserRepository cachedUserRepository;
    private final VenueRepository venueRepository;
    private final InlineKeyboardService inlineKeyboardService;
    private final LocalizationService localizationService;
    private final VersionProperties versionProperties;
    private final GitProperties gitProperties;
    private List<List<List<InlineKeyboardButton>>> pagedVenueSelectionMenu;
    private Timestamp lastPagedVenueSelectionUpdate;
    private long lastAmountOfVenuesInRepository = 0;
    private HashMap<Long, String> lastInfoMessageText = new HashMap<>();

    public UserMenuServiceImpl(CachedUserRepository cachedUserRepository, VenueRepository venueRepository, InlineKeyboardService inlineKeyboardService, LocalizationService localizationService, GitProperties gitProperties) {
        this.cachedUserRepository = cachedUserRepository;
        this.venueRepository = venueRepository;
        this.inlineKeyboardService = inlineKeyboardService;
        this.localizationService = localizationService;
        this.gitProperties = gitProperties;

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
        var number = StringUtils.getNumberFromCallbackData(sanitizedData);
        sanitizedData = StringUtils.stripNumberFromCallbackData(sanitizedData);

        if (sanitizedData.startsWith(CALLBACK_VENUE_PREFIX))
        {
            sanitizedData = sanitizedData.replace(CALLBACK_VENUE_PREFIX + "-", "");

            switch (sanitizedData) {
                case CALLBACK_VENUE_SELECTION:
                    long venueId = number;
                    var venue = venueRepository.findById(venueId);

                    if (venue.isEmpty())
                        throw new RuntimeException("Couldn't find a venue by given id!");

                    user.setSelectedVenue(venue.get());
                    user.removeState(UserState.SELECTING_VENUE);
                    cachedUserRepository.saveAndFlush(user);

                    bot.execute(new SendMessage(query.getFrom().getId().toString(),
                            StringUtils.replacePropertiesVariable("venue_name", venue.get().getName(), localizationService.getString("select.venuesuccess"))));

                    updateOrSetPinnedInfoMessage(user, bot);

                    break;

                case InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
                    if (pagedVenueSelectionMenu.size() > 1) {
                        var editVenueMenu = new EditMessageReplyMarkup();
                        editVenueMenu.setMessageId(query.getMessage().getMessageId());
                        editVenueMenu.setChatId(user.getChatId());
                        editVenueMenu.setReplyMarkup(getVenueSelectionMarkup(number));

                        bot.execute(editVenueMenu);
                    }
                    break;

                case InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
                    reply.setText(localizationService.getString("admin.pagepress"));
                    break;
            }
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

            updateOrSetPinnedInfoMessage(user, bot);
        }

        return reply;
    }

    @Override
    public boolean canCallbackMenuBeDeletedAfterHandling(CallbackQuery query) {
        var sanitizedData = StringUtils.stripNumberFromCallbackData(stripCallbackPrefix(query.getData()));

        switch (sanitizedData) {
            case CALLBACK_VENUE_PREFIX + "-" + InlineKeyboardService.CALLBACK_NAVIGATION_GETPAGE:
            case CALLBACK_VENUE_PREFIX + "-" + InlineKeyboardService.CALLBACK_NAVIGATION_PAGE:
                return false;

            default:
                return true;
        }
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
            var button = new InlineKeyboardButton(getDietString(diet));
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

        if (venueRepository.count() == 0) {
            venueSelection.setText(localizationService.getString("error.novenuesyet"));
            return venueSelection;
        }

        venueSelection.setText(localizationService.getString("select.venue"));
        venueSelection.setReplyMarkup(getVenueSelectionMarkup(0));

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
                String.format("%s\n\n%s", localizationService.getString("reply.greeting"), localizationService.getString("reply.help")));
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
        text = StringUtils.replacePropertiesVariable("technical_name", PizzaSuggesterTelegramBotMain.class.getPackageName(), text);
        text = StringUtils.replacePropertiesVariable("git_commit_hash", gitProperties.getShortCommitId(), text);
        text = StringUtils.replacePropertiesVariable("git_branch", gitProperties.getBranch(), text);
        text = StringUtils.replacePropertiesVariable("git_commit_date", gitProperties.getCommitTime().toString(), text);

        var msg = new SendMessage(user.getChatId().toString(), text);
        msg.setParseMode(ParseMode.MARKDOWNV2);
        msg.disableWebPagePreview();

        return msg;
    }

    @Override
    public BotApiMethod<?> getVenueContactInfoMessages(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException {
        if (user.getSelectedVenue() == null)
            throw new PendingVenueSelectionException("No venue selected by user yet!");

        var venue = user.getSelectedVenue();
        var venueInfo = venue.getVenueInfo();

        var sendMessage = new SendMessage();
        sendMessage.setChatId(user.getChatId());

        if (StringUtils.isNullOrEmpty(venueInfo.getAddress())
                || (venueInfo.getLatitude() == 0.0 && venueInfo.getLongitude() == 0.0)
                || StringUtils.isNullOrEmpty(venueInfo.getPhoneNumber())) {
            sendMessage.setText(localizationService.getString("error.notenoughvenuedatayet"));
            return sendMessage;
        }

        sendMessage.setText(localizationService.getString("reply.contact"));

        var venueInfoMessage = new SendVenue();
        venueInfoMessage.setChatId(user.getChatId());
        venueInfoMessage.setAddress(venue.getVenueInfo().getAddress());
        venueInfoMessage.setLatitude(venueInfo.getLatitude());
        venueInfoMessage.setLongitude(venueInfo.getLongitude());
        venueInfoMessage.setTitle(venue.getName());

        var venueContact = new SendContact();
        venueContact.setChatId(user.getChatId());
        venueContact.setPhoneNumber(venue.getVenueInfo().getPhoneNumber());
        venueContact.setFirstName(venue.getName());

        bot.execute(venueInfoMessage);
        bot.execute(sendMessage);
        return venueContact;
    }

    @Override
    public void resetPinnedInfoMessage(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException {
        user.setPinnedInfoMessageId(null);
        bot.execute(new UnpinAllChatMessages(user.getChatId().toString()));

        cachedUserRepository.save(user);
        updateOrSetPinnedInfoMessage(user, bot);
    }

    private void updateOrSetPinnedInfoMessage(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException {
        if (user.getPinnedInfoMessageId() == null) {
            setNewPinnedInfoMessage(user, bot);
            return;
        }

        try {
            var text = getInfoMessageText(user);

            if (text.equals(lastInfoMessageText.get(user.getChatId())))
                return;

            var editMessage = new EditMessageText();
            editMessage.setText(text);
            editMessage.setChatId(user.getChatId());
            editMessage.setMessageId(user.getPinnedInfoMessageId());
            editMessage.setParseMode(ParseMode.MARKDOWNV2);

            var pinMessage = new PinChatMessage();
            pinMessage.setMessageId(user.getPinnedInfoMessageId());
            pinMessage.setChatId(user.getChatId());

            lastInfoMessageText.put(user.getChatId(), text);

            bot.execute(editMessage);
            bot.execute(pinMessage);
        } catch (TelegramApiException e) {
            setNewPinnedInfoMessage(user, bot);
        }
    }

    private void setNewPinnedInfoMessage(CachedUser user, PizzaSuggesterBot bot) throws TelegramApiException {
        bot.execute(new UnpinAllChatMessages(user.getChatId().toString()));

        var text = getInfoMessageText(user);
        lastInfoMessageText.put(user.getChatId(), text);

        var message = new SendMessage();
        message.setText(text);
        message.setChatId(user.getChatId());
        message.setParseMode(ParseMode.MARKDOWNV2);

        var result = bot.execute(message);
        user.setPinnedInfoMessageId(result.getMessageId());

        cachedUserRepository.save(user);

        var pinMessage = new PinChatMessage();
        pinMessage.setMessageId(user.getPinnedInfoMessageId());
        pinMessage.setChatId(user.getChatId());

        bot.execute(pinMessage);
    }

    private String getInfoMessageText(CachedUser user) {
        var text = localizationService.getString("pinnedmessage");
        var placeholder = localizationService.getString("pinnedmessage.placeholder");
        var venueName = placeholder;
        var dietString = placeholder;

        if (user.getSelectedVenue() != null) {
            venueName = user.getSelectedVenue().getName();
        }

        if (user.getUserDiet() != null) {
            dietString = getDietString(user.getUserDiet());
        }

        text = StringUtils.replacePropertiesVariable("venue_name", StringUtils.escapeForMarkdownV2Format(venueName), text);
        text = StringUtils.replacePropertiesVariable("diet", StringUtils.escapeForMarkdownV2Format(dietString), text);

        return text;
    }

    private InlineKeyboardMarkup getVenueSelectionMarkup(int page) {
        var keyboard = new InlineKeyboardMarkup();
        regenerateMenuCaches();

        keyboard.setKeyboard(pagedVenueSelectionMenu.get(page));

        return keyboard;
    }

    private String formatVenueForButton(Venue venue) {
        var venueInfoText = localizationService.getString("venue.info");
        venueInfoText = StringUtils.replacePropertiesVariable("venue_name", venue.getName(), venueInfoText);

        var venueAddress = localizationService.getString("venue.address.fallback");

        if (!StringUtils.isNullOrEmpty(venue.getVenueInfo().getAddress()))
            venueAddress = venue.getVenueInfo().getAddress();

        venueInfoText = StringUtils.replacePropertiesVariable("venue_address", venueAddress, venueInfoText);

        var additionalInfoText = "";

        if (venue.supportsGlutenFree() && venue.supportsLactoseFree())
            additionalInfoText = " | " + localizationService.getString("picto.glutenandlactosefree");
        else if (venue.supportsGlutenFree() && !venue.supportsLactoseFree())
            additionalInfoText = " | " +localizationService.getString("picto.glutenfree");
        else if (!venue.supportsGlutenFree() && venue.supportsLactoseFree())
            additionalInfoText = " | " +localizationService.getString("picto.lactosefree");

        venueInfoText = StringUtils.replacePropertiesVariable("venue_additional_info", additionalInfoText, venueInfoText);

        return venueInfoText;
    }

    private List<InlineKeyboardButton> getVenueButtons() {
        var venueButtons = new ArrayList<InlineKeyboardButton>();

        for (Venue venue : venueRepository.findAll())
        {
            var button = new InlineKeyboardButton(formatVenueForButton(venue));
            button.setCallbackData(prependCallbackPrefix(StringUtils.appendNumberToCallbackData(CALLBACK_VENUE_PREFIX + "-" + CALLBACK_VENUE_SELECTION, Math.toIntExact(venue.getId()))));
            venueButtons.add(button);
        }

        return venueButtons;
    }

    private String getDietString(UserDiet diet) {
        return localizationService.getString(String.format("%s.%s", MESSAGES_LABEL_DIET_PREFIX, diet.name().toLowerCase()));
    }

    private void regenerateMenuCaches() {
        if (!venueRepository.findAll().isEmpty() &&
                (pagedVenueSelectionMenu == null ||
                pagedVenueSelectionMenu.isEmpty() ||
                lastPagedVenueSelectionUpdate == null ||
                venueRepository.existsByModifiedAtAfter(lastPagedVenueSelectionUpdate) ||
                venueRepository.existsByVenueInfoModifiedAtAfter(lastPagedVenueSelectionUpdate) ||
                lastAmountOfVenuesInRepository != venueRepository.count())
        ) {
            pagedVenueSelectionMenu = inlineKeyboardService.getPagedInlineKeyboardButtonsWithFooterAndCloseButton(getVenueButtons(), VENUE_SELECTION_COLUMNS, VENUE_SELECTION_ROWS, getCallbackPrefix() + "-" + CALLBACK_VENUE_PREFIX);
            lastPagedVenueSelectionUpdate = Timestamp.from(Instant.now());
            lastAmountOfVenuesInRepository = venueRepository.count();
        }
    }
}
