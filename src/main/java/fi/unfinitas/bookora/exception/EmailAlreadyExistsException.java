package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when attempting to register with an email that already exists.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(final String message) {
        super(message);
    }

    public EmailAlreadyExistsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
