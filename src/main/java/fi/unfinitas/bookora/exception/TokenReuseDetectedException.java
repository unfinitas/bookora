package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a refresh token reuse is detected.
 * This is a serious security event that indicates possible token theft.
 */
public class TokenReuseDetectedException extends RuntimeException {

    public TokenReuseDetectedException(final String message) {
        super(message);
    }

    public TokenReuseDetectedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}