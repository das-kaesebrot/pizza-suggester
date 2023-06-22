package eu.kaesebrot.dev.pizzabot.exceptions;

public class NoPizzasFoundException extends RuntimeException {
    public NoPizzasFoundException() {
    }

    public NoPizzasFoundException(String message) {
        super(message);
    }

    public NoPizzasFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
