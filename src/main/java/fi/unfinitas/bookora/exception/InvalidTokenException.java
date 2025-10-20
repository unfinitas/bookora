package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a guest access token is invalid or does not exist.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(final String message) {
        super(message);
    }

    public InvalidTokenException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
