package fi.unfinitas.bookora.exception;

public class VerificationTokenExpiredException extends RuntimeException {

    public VerificationTokenExpiredException(final String message) {
        super(message);
    }

    public VerificationTokenExpiredException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
