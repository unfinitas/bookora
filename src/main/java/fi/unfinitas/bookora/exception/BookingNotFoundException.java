package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a booking is not found.
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(final String message) {
        super(message);
    }

    public BookingNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
