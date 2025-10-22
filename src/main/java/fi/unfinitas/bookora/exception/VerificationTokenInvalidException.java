package fi.unfinitas.bookora.exception;

public class VerificationTokenInvalidException extends RuntimeException {

    public VerificationTokenInvalidException(final String message) {
        super(message);
    }

    public VerificationTokenInvalidException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
