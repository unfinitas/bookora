package fi.unfinitas.bookora.exception;

public class PasswordResetTokenInvalidException extends RuntimeException {

    public PasswordResetTokenInvalidException(final String message) {
        super(message);
    }

    public PasswordResetTokenInvalidException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
