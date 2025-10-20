package fi.unfinitas.bookora.exception;

/**
 * Exception thrown when a service is not found.
 */
public class ServiceNotFoundException extends RuntimeException {

    public ServiceNotFoundException(final String message) {
        super(message);
    }

    public ServiceNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
