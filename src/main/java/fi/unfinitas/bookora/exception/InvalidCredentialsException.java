package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when login credentials are invalid.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(final String message) {
        super(message);
    }

    public InvalidCredentialsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
