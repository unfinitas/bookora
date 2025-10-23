package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when attempting to confirm a booking that is already confirmed.
 */
public class BookingAlreadyConfirmedException extends RuntimeException {

    public BookingAlreadyConfirmedException(final String message) {
        super(message);
    }

    public BookingAlreadyConfirmedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
