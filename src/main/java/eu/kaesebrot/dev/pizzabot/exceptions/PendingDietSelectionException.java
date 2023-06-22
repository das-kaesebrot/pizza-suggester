package eu.kaesebrot.dev.pizzabot.exceptions;

public class PendingDietSelectionException extends PendingSelectionException {
    public PendingDietSelectionException(String errorMessage) {
        super(errorMessage);
    }

    public PendingDietSelectionException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
