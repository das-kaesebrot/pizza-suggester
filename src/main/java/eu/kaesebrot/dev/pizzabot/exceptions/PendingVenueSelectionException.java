package eu.kaesebrot.dev.pizzabot.exceptions;

public class PendingVenueSelectionException extends PendingSelectionException {
    public PendingVenueSelectionException(String errorMessage) {
        super(errorMessage);
    }

    public PendingVenueSelectionException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
