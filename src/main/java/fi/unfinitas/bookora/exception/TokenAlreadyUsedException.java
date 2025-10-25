package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a guest access token has already been used.
 */
public class TokenAlreadyUsedException extends RuntimeException {

    public TokenAlreadyUsedException(final String message) {
        super(message);
    }

    public TokenAlreadyUsedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
