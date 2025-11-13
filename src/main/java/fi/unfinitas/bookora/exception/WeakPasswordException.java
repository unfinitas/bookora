package fi.unfinitas.bookora.exception;

public class WeakPasswordException extends RuntimeException {

    public WeakPasswordException(final String message) {
        super(message);
    }

    public WeakPasswordException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
