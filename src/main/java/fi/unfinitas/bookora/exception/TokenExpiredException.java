package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a guest access token has expired.
 */
public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(final String message) {
        super(message);
    }

    public TokenExpiredException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
