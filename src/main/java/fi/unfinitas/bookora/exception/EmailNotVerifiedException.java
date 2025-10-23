package fi.unfinitas.bookora.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(final String message) {
        super(message);
    }

    public EmailNotVerifiedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
