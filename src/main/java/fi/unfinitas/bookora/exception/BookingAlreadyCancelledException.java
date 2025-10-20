package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when attempting to confirm a booking that has already been cancelled.
 */
public class BookingAlreadyCancelledException extends RuntimeException {

    public BookingAlreadyCancelledException(final String message) {
        super(message);
    }

    public BookingAlreadyCancelledException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
