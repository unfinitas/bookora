package fi.unfinitas.bookora.exception;

public class PasswordResetTokenExpiredException extends RuntimeException {

    public PasswordResetTokenExpiredException(final String message) {
        super(message);
    }

    public PasswordResetTokenExpiredException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
