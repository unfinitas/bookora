package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when booking time is invalid.
 * This includes past time bookings, end time before start time, and overlapping bookings.
 */
public class InvalidBookingTimeException extends RuntimeException {

    public InvalidBookingTimeException(final String message) {
        super(message);
    }

    public InvalidBookingTimeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
