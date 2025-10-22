package fi.unfinitas.bookora.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(final String message) {
        super(message);
    }

    public RateLimitExceededException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
