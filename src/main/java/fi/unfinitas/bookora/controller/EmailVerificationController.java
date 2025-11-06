package fi.unfinitas.bookora.controller;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.dto.request.ResendVerificationRequest;
import fi.unfinitas.bookora.dto.response.ApiResponse;
import fi.unfinitas.bookora.dto.response.VerifyEmailResponse;
import fi.unfinitas.bookora.service.EmailVerificationService;
import fi.unfinitas.bookora.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

/**
 * Controller for email verification operations.
 * All endpoints are public (no authentication required).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Verification", description = "Public endpoints for email verification (no authentication required)")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final UserService userService;
    private final BookoraProperties bookoraProperties;

    /**
     * Verify email address using token from email.
     * Marks user's email as verified and redirects to frontend.
     *
     * @param token the UUID verification token from email
     * @return redirect to frontend URL with success or error status
     */
    @GetMapping("/verify/{token}")
    @Operation(summary = "Verify email", description = "Verify email address using token sent to user's email. Marks email as verified and redirects to frontend.")
    public RedirectView verifyEmail(@PathVariable final UUID token) {
        log.debug("Verifying email with token");

        try {
            emailVerificationService.verifyEmail(token);
            log.info("Email verified successfully. Redirecting to frontend.");

            // Redirect to frontend with success parameter
            return new RedirectView(bookoraProperties.getFrontendUrl() + "?verified=true");

        } catch (final Exception e) {
            log.error("Email verification failed: {}", e.getMessage());

            // Redirect to frontend with error parameter
            return new RedirectView(bookoraProperties.getFrontendUrl() + "?verified=false&error=" + e.getMessage());
        }
    }

    /**
     * Resend verification email to user.
     * Rate limited to 1 request per hour per user.
     *
     * @param request the resend request containing user's email
     * @return success response
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email", description = "Resend verification email. Rate limited to 1 request per hour.")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail(
            @Valid @RequestBody final ResendVerificationRequest request) {
        log.debug("Resending verification email to: {}", request.email());

        emailVerificationService.resendVerificationEmail(request.email());

        final ApiResponse<String> response = ApiResponse.success(
                "Verification email sent successfully",
                "Please check your email for the verification link"
        );

        return ResponseEntity.ok(response);
    }
}
