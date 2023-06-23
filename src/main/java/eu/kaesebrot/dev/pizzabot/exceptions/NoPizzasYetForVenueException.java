package eu.kaesebrot.dev.pizzabot.exceptions;

import eu.kaesebrot.dev.pizzabot.model.Venue;

public class NoPizzasYetForVenueException extends RuntimeException {
    private final Venue venue;
    public NoPizzasYetForVenueException(Venue venue) {
        this.venue = venue;
    }

    public Venue getVenue() {
        return venue;
    }
}
