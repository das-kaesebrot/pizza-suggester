package eu.kaesebrot.dev.pizzabot.enums;

public enum UserState {
    FREE,
    SELECTING_VENUE,
    SELECTING_DIET,
    SELECTING_PIZZAS,

    // limited to non-admin users
    SENDING_ADMIN_KEY,

    // Limited to admin users
    CREATING_VENUE,
    MODIFYING_VENUE,
    SENDING_VENUE_NAME,
    SENDING_VENUE_ADDRESS,
    SENDING_VENUE_PHONE_NUMBER,
    SENDING_VENUE_URL,
    SENDING_VENUE_CSV,
    SENDING_VENUE_EXTRA_INGREDIENT_CSV,
}