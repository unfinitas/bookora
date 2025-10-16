package fi.unfinitas.bookora.exception;

import fi.unfinitas.bookora.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle validation exceptions")
    void shouldHandleValidationExceptions() {
        final BindingResult bindingResult = mock(BindingResult.class);
        final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        final FieldError fieldError1 = new FieldError("registerRequest", "username", "Username is required");
        final FieldError fieldError2 = new FieldError("registerRequest", "email", "Email is invalid");

        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        final ResponseEntity<ApiResponse<Map<String, String>>> response = globalExceptionHandler.handleValidationExceptions(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("FAIL");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getData()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(2);
        assertThat(response.getBody().getData().get("username")).isEqualTo("Username is required");
        assertThat(response.getBody().getData().get("email")).isEqualTo("Email is invalid");
    }

    @Test
    @DisplayName("Should handle email already exists exception")
    void shouldHandleEmailAlreadyExistsException() {
        final EmailAlreadyExistsException ex = new EmailAlreadyExistsException("Email is already registered");

        final ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleEmailAlreadyExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("FAIL");
        assertThat(response.getBody().getMessage()).isEqualTo("Email is already registered");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("Should handle username already exists exception")
    void shouldHandleUsernameAlreadyExistsException() {
        final UsernameAlreadyExistsException ex = new UsernameAlreadyExistsException("Username is already taken");

        final ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleUsernameAlreadyExists(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("FAIL");
        assertThat(response.getBody().getMessage()).isEqualTo("Username is already taken");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("Should handle invalid credentials exception")
    void shouldHandleInvalidCredentialsException() {
        final InvalidCredentialsException ex = new InvalidCredentialsException("Invalid username or password");

        final ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleInvalidCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("FAIL");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("Should handle username not found exception")
    void shouldHandleUsernameNotFoundException() {
        final UsernameNotFoundException ex = new UsernameNotFoundException("User not found");

        final ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleUsernameNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("FAIL");
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("Should handle generic exception")
    void shouldHandleGenericException() {
        final Exception ex = new RuntimeException("Unexpected error");

        final ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("Should handle null pointer exception as generic exception")
    void shouldHandleNullPointerExceptionAsGenericException() {
        final NullPointerException ex = new NullPointerException("Null value encountered");

        final ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    @DisplayName("Should handle illegal argument exception as generic exception")
    void shouldHandleIllegalArgumentExceptionAsGenericException() {
        final IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        final ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Should handle missing request header exception")
    void shouldHandleMissingRequestHeaderException() {
        final MissingRequestHeaderException ex = new MissingRequestHeaderException(
                "Authorization",
                null
        );

        final ResponseEntity<ApiResponse<Void>> response = globalExceptionHandler.handleMissingRequestHeader(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("FAIL");
        assertThat(response.getBody().getMessage()).contains("Authorization");
        assertThat(response.getBody().getMessage()).contains("missing");
    }
}
