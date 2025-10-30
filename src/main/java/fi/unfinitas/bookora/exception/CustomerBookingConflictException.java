package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a customer attempts to book multiple overlapping appointments.
 * A customer cannot be in two places at the same time.
 */
public class CustomerBookingConflictException extends RuntimeException {

    public CustomerBookingConflictException(final String message) {
        super(message);
    }

    public CustomerBookingConflictException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
