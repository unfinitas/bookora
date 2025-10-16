package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when attempting to register with a username that already exists.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(final String message) {
        super(message);
    }

    public UsernameAlreadyExistsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
