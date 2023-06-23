package eu.kaesebrot.dev.pizzabot.exceptions;

public class MalformedDataException extends RuntimeException {
    public MalformedDataException() {

    }
    public MalformedDataException(String message) {
        super(message);
    }

    public MalformedDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
