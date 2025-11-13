package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import fi.unfinitas.bookora.domain.model.PasswordResetToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.ForgotPasswordRequest;
import fi.unfinitas.bookora.dto.request.ResetPasswordDto;
import fi.unfinitas.bookora.exception.PasswordResetTokenAlreadyUsedException;
import fi.unfinitas.bookora.exception.PasswordResetTokenExpiredException;
import fi.unfinitas.bookora.exception.PasswordResetTokenInvalidException;
import fi.unfinitas.bookora.exception.RateLimitExceededException;
import fi.unfinitas.bookora.exception.UserNotFoundException;
import fi.unfinitas.bookora.repository.PasswordResetTokenRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserService userService;
    private final PasswordResetTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BookoraProperties bookoraProperties;

    @Transactional
    @RateLimiter(name = "passwordResetRequest")
    public void requestPasswordReset(final ForgotPasswordRequest request) {
        final User user = userService.findByEmail(request.email());

        checkUserRateLimit(user);

        tokenRepository.deleteByUserId(user.getId());

        final PasswordResetToken token = generateResetToken(user);

        publishPasswordResetEmail(user, token);

        userService.updateLastPasswordResetEmailSentAt(user.getId());
    }

    @Transactional
    public void resetPassword(final ResetPasswordDto request) {
        final PasswordResetToken token = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new PasswordResetTokenInvalidException("Invalid password reset token"));

        validateToken(token);

        userService.updatePassword(token.getUserId(), request.newPassword());

        token.markAsUsed();
        tokenRepository.save(token);
    }

    private void checkUserRateLimit(final User user) {
        if (user.getLastPasswordResetEmailSentAt() != null) {
            final LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            if (user.getLastPasswordResetEmailSentAt().isAfter(oneHourAgo)) {
                throw new RateLimitExceededException(
                        "Please wait before requesting another password reset email");
            }
        }
    }

    private PasswordResetToken generateResetToken(final User user) {
        final UUID token = UUID.randomUUID();
        final int expirationHours = bookoraProperties.getPasswordReset().getToken().getExpirationHours();
        final LocalDateTime expiresAt = LocalDateTime.now().plusHours(expirationHours);

        final PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(expiresAt)
                .build();

        return tokenRepository.save(resetToken);
    }

    private void validateToken(final PasswordResetToken token) {
        if (token.isUsed()) {
            throw new PasswordResetTokenAlreadyUsedException(
                    "This password reset token has already been used");
        }

        if (token.isExpired()) {
            throw new PasswordResetTokenExpiredException(
                    "Password reset token has expired. Please request a new one.");
        }
    }

    private void publishPasswordResetEmail(final User user, final PasswordResetToken token) {
        final String resetLink = bookoraProperties.getFrontendUrl() +
                "/reset-password?token=" + token.getToken();

        final Map<String, Object> variables = Map.of(
                "firstName", user.getFirstName(),
                "resetLink", resetLink,
                "expirationHours", bookoraProperties.getPasswordReset().getToken().getExpirationHours(),
                "frontendUrl", bookoraProperties.getFrontendUrl()
        );

        final SendMailEvent event = new SendMailEvent(
                user.getEmail(),
                "Password Reset Request - Bookora",
                "email/password-reset",
                variables
        );

        eventPublisher.publishEvent(event);
    }
}
