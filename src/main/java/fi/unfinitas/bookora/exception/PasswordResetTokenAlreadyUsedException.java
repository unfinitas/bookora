package fi.unfinitas.bookora.exception;

public class PasswordResetTokenAlreadyUsedException extends RuntimeException {

    public PasswordResetTokenAlreadyUsedException(final String message) {
        super(message);
    }

    public PasswordResetTokenAlreadyUsedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
