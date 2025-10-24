package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a guest tries to book with an email that belongs to a registered user.
 * The user should log in instead of booking as a guest.
 */
public class GuestEmailAlreadyRegisteredException extends RuntimeException {

    public GuestEmailAlreadyRegisteredException(final String message) {
        super(message);
    }

    public GuestEmailAlreadyRegisteredException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
