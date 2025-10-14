package fi.unfinitas.bookora.controller;

import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.ApiResponse;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import fi.unfinitas.bookora.exception.InvalidCredentialsException;
import fi.unfinitas.bookora.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints (registration and login).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication endpoints for registration and login")
public class AuthController {

    private final AuthenticationService authenticationService;

    /**
     * Register a new user.
     *
     * @param request the registration request
     * @return the registration response without tokens
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a new user. Login separately to get tokens.")
    public ResponseEntity<ApiResponse<UserPublicInfo>> register(@Valid @RequestBody final RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        final UserPublicInfo data = authenticationService.register(request);
        final ApiResponse<UserPublicInfo> response = ApiResponse.success("User registration successful", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login a user.
     *
     * @param request the login request
     * @return the authentication response with tokens
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody final LoginRequest request) {
        log.info("Login request received for username: {}", request.username());
        final LoginResponse data = authenticationService.login(request);
        final ApiResponse<LoginResponse> response = ApiResponse.success("Login successful", data);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token.
     *
     * @param authHeader the authorization header (must start with "Bearer ")
     * @return the API response with new tokens
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @RequestHeader("Authorization") final String authHeader) {
        log.info("Token refresh request received");

        validateBearerToken(authHeader);

        final String token = authHeader.substring(7);
        final LoginResponse data = authenticationService.refreshToken(token);
        final ApiResponse<LoginResponse> response = ApiResponse.success("Token refreshed successfully", data);
        return ResponseEntity.ok(response);
    }

    private static void validateBearerToken(final String authHeader) {
        if (!authHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException(
                "Authorization header must start with 'Bearer '"
            );
        }
    }
}
