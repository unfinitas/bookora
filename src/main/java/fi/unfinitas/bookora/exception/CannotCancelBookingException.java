package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when attempting to cancel a booking that cannot be cancelled.
 * This includes bookings within 24 hours of start time or already cancelled bookings.
 */
public class CannotCancelBookingException extends RuntimeException {

    public CannotCancelBookingException(final String message) {
        super(message);
    }

    public CannotCancelBookingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
