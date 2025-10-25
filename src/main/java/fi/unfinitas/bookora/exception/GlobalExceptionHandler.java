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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(final EmailAlreadyExistsException ex) {
        log.debug("Email already exists: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameAlreadyExists(final UsernameAlreadyExistsException ex) {
        log.debug("Username already exists: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(final InvalidCredentialsException ex) {
        log.warn("Invalid credentials attempt: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFound(final UsernameNotFoundException ex) {
        log.warn("Username not found: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(final UserNotFoundException ex) {
        log.debug("User not found: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(final MissingRequestHeaderException ex) {
        log.debug("Missing request header: {}", ex.getHeaderName());
        final ApiResponse<Void> response = ApiResponse.fail("Required header '" + ex.getHeaderName() + "' is missing");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingNotFound(final BookingNotFoundException ex) {
        log.debug("Booking not found: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceNotFound(final ServiceNotFoundException ex) {
        log.debug("Service not found: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidBookingTimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidBookingTime(final InvalidBookingTimeException ex) {
        log.debug("Invalid booking time: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpired(final TokenExpiredException ex) {
        log.debug("Token expired: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidToken(final InvalidTokenException ex) {
        log.debug("Invalid token: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenAlreadyUsedException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenAlreadyUsed(final TokenAlreadyUsedException ex) {
        log.warn("Token already used: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(GuestEmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse<Void>> handleGuestEmailAlreadyRegistered(final GuestEmailAlreadyRegisteredException ex) {
        log.debug("Guest email already registered: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BookingAlreadyConfirmedException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingAlreadyConfirmed(final BookingAlreadyConfirmedException ex) {
        log.debug("Booking already confirmed: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BookingAlreadyCancelledException.class)
    public ResponseEntity<ApiResponse<Void>> handleBookingAlreadyCancelled(final BookingAlreadyCancelledException ex) {
        log.debug("Booking already cancelled: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CannotCancelBookingException.class)
    public ResponseEntity<ApiResponse<Void>> handleCannotCancelBooking(final CannotCancelBookingException ex) {
        log.debug("Cannot cancel booking: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailNotVerified(final EmailNotVerifiedException ex) {
        log.debug("Email not verified: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(VerificationTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleVerificationTokenExpired(final VerificationTokenExpiredException ex) {
        log.debug("Verification token expired: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.GONE);
    }

    @ExceptionHandler(VerificationTokenInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleVerificationTokenInvalid(final VerificationTokenInvalidException ex) {
        log.debug("Verification token invalid: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(final RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        final ApiResponse<Void> response = ApiResponse.fail(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(final Exception ex) {
        log.error("Unexpected error occurred", ex);
        final ApiResponse<Void> response = ApiResponse.error("An unexpected error occurred");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
