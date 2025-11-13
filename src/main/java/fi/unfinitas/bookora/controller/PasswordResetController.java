package fi.unfinitas.bookora.controller;

import fi.unfinitas.bookora.dto.request.ForgotPasswordRequest;
import fi.unfinitas.bookora.dto.request.ResetPasswordDto;
import fi.unfinitas.bookora.dto.response.ApiResponse;
import fi.unfinitas.bookora.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for password reset operations.
 * All endpoints are public (no authentication required).
 */
@RestController
@RequestMapping("/auth/password-reset")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Password Reset", description = "Public endpoints for password reset (no authentication required)")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    /**
     * Request password reset email.
     * Sends password reset link to user's email if account exists.
     * Rate limited to 3 requests per hour.
     *
     * @param request the forgot password request containing user's email
     * @return success response (same response whether user exists or not for security)
     */
    @PostMapping("/request")
    @Operation(summary = "Request password reset", description = "Send password reset email. Rate limited to 3 requests per hour.")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(
            @Valid @RequestBody final ForgotPasswordRequest request) {
        log.debug("Password reset requested for email: {}", request.email());

        passwordResetService.requestPasswordReset(request);

        final ApiResponse<Void> response = ApiResponse.success("If an account exists with this email, a password reset link has been sent.");

        return ResponseEntity.ok(response);
    }

    /**
     * Reset password using token from email.
     * Validates token and updates user's password.
     *
     * @param request the reset password request containing token and new password
     * @return success response
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset password", description = "Reset password using token sent to email")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody final ResetPasswordDto request) {
        log.debug("Password reset attempt with token");

        passwordResetService.resetPassword(request);

        final ApiResponse<Void> response = ApiResponse.success("Your password has been successfully reset.");

        return ResponseEntity.ok(response);
    }
}
