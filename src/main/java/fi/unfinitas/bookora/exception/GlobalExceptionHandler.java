package fi.unfinitas.bookora.exception;

import fi.unfinitas.bookora.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

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
     * Handle booking not found exception.
     */
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingNotFound(final BookingNotFoundException ex) {
        log.debug("Booking not found: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle service not found exception.
     */
    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceNotFound(final ServiceNotFoundException ex) {
        log.debug("Service not found: {}", ex.getMessage());
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
     * Handle generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(final Exception ex) {
        log.error("Unexpected error occurred", ex);
        final ApiResponse<Void> response = ApiResponse.error("An unexpected error occurred");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
