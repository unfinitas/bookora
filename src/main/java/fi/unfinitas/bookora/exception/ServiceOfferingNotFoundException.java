package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a service offering is not found.
 */
public class ServiceOfferingNotFoundException extends RuntimeException {

    public ServiceOfferingNotFoundException(final String message) {
        super(message);
    }

    public ServiceOfferingNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
