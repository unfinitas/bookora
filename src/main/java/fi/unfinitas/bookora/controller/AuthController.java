package fi.unfinitas.bookora.controller;

import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.ApiResponse;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import fi.unfinitas.bookora.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        final UserPublicInfo data = authenticationService.register(request);
        final ApiResponse<UserPublicInfo> response = ApiResponse.success("User registration successful", data);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login a user.
     *
     * @param request the login request
     * @param response the HTTP response for setting cookies
     * @return the authentication response with access token only
     */
    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate user and return access token (refresh token in HttpOnly cookie)")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody final LoginRequest request,
            HttpServletResponse response) {

        final LoginResponse data = authenticationService.login(request, response);
        final ApiResponse<LoginResponse> apiResponse = ApiResponse.success("Login successful", data);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Refresh access token using refresh token from cookie.
     *
     * @param request the HTTP request to get refresh token from cookie
     * @param response the HTTP response for setting new refresh token cookie
     * @return the API response with new access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token from cookie")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        final LoginResponse data = authenticationService.refreshToken(request, response);
        final ApiResponse<LoginResponse> apiResponse = ApiResponse.success("Token refreshed successfully", data);
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Logout user by revoking refresh token.
     *
     * @param request the HTTP request to get refresh token from cookie
     * @param response the HTTP response for clearing cookie
     * @return the API response
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout user and revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        authenticationService.logout(request, response);
        final ApiResponse<Void> apiResponse = ApiResponse.success("Logout successful", null);
        return ResponseEntity.ok(apiResponse);
    }
}
