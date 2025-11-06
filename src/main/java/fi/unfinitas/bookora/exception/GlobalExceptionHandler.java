package fi.unfinitas.bookora.exception;

import fi.unfinitas.bookora.dto.response.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the application.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(final MethodArgumentNotValidException ex) {
        final Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            final String fieldName = ((FieldError) error).getField();
            final String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.debug("Validation failed: {}", errors);
        final ApiResponse<Map<String, String>> response = ApiResponse.fail("Validation failed", errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle email already exists exception.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(final EmailAlreadyExistsException ex) {
        log.debug("Email already exists: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle username already exists exception.
     */
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameAlreadyExists(final UsernameAlreadyExistsException ex) {
        log.debug("Username already exists: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle invalid credentials' exception.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(final InvalidCredentialsException ex) {
        log.warn("Invalid credentials attempt: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle username not found exception.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFound(final UsernameNotFoundException ex) {
        log.warn("Username not found: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle missing request header exception.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(final MissingRequestHeaderException ex) {
        log.debug("Missing request header: {}", ex.getHeaderName());
        final ApiResponse<Void> response = ApiResponse.fail("Required header '" + ex.getHeaderName() + "' is missing");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle method argument type mismatch exception (e.g., invalid UUID format).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        final String paramName = ex.getName();
        final Object value = ex.getValue();
        final Class<?> requiredType = ex.getRequiredType();

        String errorMessage = String.format("Invalid value '%s' for parameter '%s'", value, paramName);

        if (requiredType != null && requiredType.equals(UUID.class)) {
            errorMessage = "Invalid token format. Token must be a valid UUID.";
            log.debug("Invalid UUID format for parameter '{}': {}", paramName, value);
        } else {
            log.debug("Type mismatch for parameter '{}': expected {}, got {}", paramName, requiredType, value);
        }

        final ApiResponse<Void> response = ApiResponse.fail(errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle booking not found exception.
     */
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingNotFound(final BookingNotFoundException ex) {
        log.debug("Booking not found: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle service offering not found exception.
     */
    @ExceptionHandler(ServiceOfferingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceOfferingNotFound(final ServiceOfferingNotFoundException ex) {
        log.debug("Service offering not found: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle invalid booking time exception.
     */
    @ExceptionHandler(InvalidBookingTimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBookingTime(final InvalidBookingTimeException ex) {
        log.debug("Invalid booking time: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle token expired exception.
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpired(final TokenExpiredException ex) {
        log.debug("Token expired: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle invalid token exception.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(final InvalidTokenException ex) {
        log.debug("Invalid token: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle token already used exception.
     */
    @ExceptionHandler(TokenAlreadyUsedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenAlreadyUsed(final TokenAlreadyUsedException ex) {
        log.warn("Token already used: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle guest email already registered exception.
     */
    @ExceptionHandler(GuestEmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse<Void>> handleGuestEmailAlreadyRegistered(final GuestEmailAlreadyRegisteredException ex) {
        log.debug("Guest email already registered: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle booking already confirmed exception.
     */
    @ExceptionHandler(BookingAlreadyConfirmedException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingAlreadyConfirmed(final BookingAlreadyConfirmedException ex) {
        log.debug("Booking already confirmed: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle booking already cancelled exception.
     */
    @ExceptionHandler(BookingAlreadyCancelledException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingAlreadyCancelled(final BookingAlreadyCancelledException ex) {
        log.debug("Booking already cancelled: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle cannot cancel booking exception.
     */
    @ExceptionHandler(CannotCancelBookingException.class)
    public ResponseEntity<ApiResponse<Void>> handleCannotCancelBooking(final CannotCancelBookingException ex) {
        log.debug("Cannot cancel booking: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle customer booking conflict exception.
     */
    @ExceptionHandler(CustomerBookingConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomerBookingConflict(final CustomerBookingConflictException ex) {
        log.debug("Customer booking conflict: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle optimistic locking failures.
     * Occurs when two users/threads try to modify the same entity simultaneously.
     * The version field prevents lost updates by detecting concurrent modifications.
     */
    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockException(final Exception ex) {
        log.warn("Optimistic lock conflict detected: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(
                "This record was modified by another user. Please refresh and try again."
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(final Exception ex) {
        log.error("Unexpected error occurred", ex);
        final ApiResponse<Void> response = ApiResponse.error("An unexpected error occurred");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle DataIntegrityViolation except booking overlap
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            final DataIntegrityViolationException ex) {
        final String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // Check if this is a foreign key constraint violation
        if (message.contains("foreign key") || message.contains("violates") || message.contains("referenced")) {
            log.warn("Foreign key constraint violation: Cannot delete entity with associated data", ex);
            final ApiResponse<Void> response = ApiResponse.fail(
                    "Cannot delete this record because it has associated data. " +
                    "Please remove or reassign the related records first."
            );
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        log.error("Database constraint violation", ex);
        final ApiResponse<Void> response = ApiResponse.fail(
                "The request could not be completed due to a data constraint violation."
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
