package eu.kaesebrot.dev.enums;

public enum UserState {
    FREE,
    SELECTING_VENUE,
    SENDING_ADMIN_KEY,

    // Limited to admin users
    SENDING_VENUE_CSV,
    SENDING_VENUE_EXTRA_INGREDIENT_CSV,
}
