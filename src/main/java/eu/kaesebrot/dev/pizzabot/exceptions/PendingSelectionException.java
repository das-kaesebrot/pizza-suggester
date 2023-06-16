package eu.kaesebrot.dev.pizzabot.exceptions;

public class PendingSelectionException extends RuntimeException {
    public PendingSelectionException(String errorMessage) {
        super(errorMessage);
    }

    public PendingSelectionException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
