package eu.kaesebrot.dev.pizzabot.enums;

public enum UserState {
    FREE,
    SELECTING_VENUE,
    SELECTING_DIET,
    SELECTING_PIZZAS,

    // limited to non-admin users
    SENDING_ADMIN_KEY,

    // Limited to admin users
    SENDING_VENUE_CSV,
    SENDING_VENUE_EXTRA_INGREDIENT_CSV,
}
