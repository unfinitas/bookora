package fi.unfinitas.bookora.service.impl;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.exception.RateLimitExceededException;
import fi.unfinitas.bookora.exception.UserNotFoundException;
import fi.unfinitas.bookora.exception.VerificationTokenExpiredException;
import fi.unfinitas.bookora.exception.VerificationTokenInvalidException;
import fi.unfinitas.bookora.repository.EmailVerificationTokenRepository;
import fi.unfinitas.bookora.repository.UserRepository;
import fi.unfinitas.bookora.service.EmailVerificationService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BookoraProperties bookoraProperties;

    @Override
    @Transactional
    public EmailVerificationToken generateVerificationToken(final User user) {
        log.debug("Generating verification token for user ID: {}", user.getId());

        final int expirationDays = getExpirationDays();

        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .build();

        final EmailVerificationToken savedToken = tokenRepository.save(token);
        log.debug("Verification token generated successfully for user ID: {}", user.getId());

        return savedToken;
    }

    @Override
    @Transactional
    public void verifyEmail(final UUID token) {
        log.debug("Verifying email with token");

        final EmailVerificationToken verificationToken = findToken(token);

        isExpired(verificationToken);

        isUsed(verificationToken);

        final User user = findUser(verificationToken);

        user.setIsEmailVerified(true);
        verificationToken.markAsUsed();

        userRepository.save(user);
        tokenRepository.save(verificationToken);

        log.info("Email verified successfully for user ID: {}", user.getId());
    }

    @Override
    @Transactional
    @RateLimiter(name = "emailVerificationResend")
    public void resendVerificationEmail(final String email) {
        log.debug("Resending verification email to: {}", email);

        final User user = findUser(email);

        canResendEmail(user);

        tokenRepository.deleteByUserId(user.getId());
        log.debug("Deleted old verification tokens for user ID: {}", user.getId());

        final EmailVerificationToken newToken = generateVerificationToken(user);

        user.setLastVerificationEmailSentAt(LocalDateTime.now());
        userRepository.save(user);

        publishVerificationEmail(user, newToken);

        log.info("Verification email resent successfully to: {}", email);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UserNotFoundException("User not found with email: " + email);
                });
    }

    private static void canResendEmail(User user) {
        if (!user.canResendVerificationEmail()) {
            log.warn("Rate limit exceeded for user ID: {}", user.getId());
            throw new RateLimitExceededException("Please wait at least 1 hour before requesting another verification email");
        }
    }

    private void publishVerificationEmail(final User user, final EmailVerificationToken token) {
        try {
            final int expirationDays = getExpirationDays();

            final Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("firstName", user.getFirstName());
            templateVariables.put("verificationLink", bookoraProperties.getBackendUrl() + "/auth/verify/" + token.getToken());
            templateVariables.put("expirationDays", expirationDays);
            templateVariables.put("frontendUrl", bookoraProperties.getFrontendUrl());

            final SendMailEvent event = new SendMailEvent(
                    user.getEmail(),
                    "Verify Your Email - Bookora",
                    "email/email-verification",
                    templateVariables
            );

            eventPublisher.publishEvent(event);
            log.debug("Published SendMailEvent for email verification to user ID: {}", user.getId());
        } catch (final Exception e) {
            log.error("Failed to publish SendMailEvent for user ID: {}", user.getId(), e);
        }
    }

    private EmailVerificationToken findToken(UUID token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Verification token not found");
                    return new VerificationTokenInvalidException("Invalid verification token");
                });
    }

    private User findUser(EmailVerificationToken verificationToken) {
        return userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found for token user ID: {}", verificationToken.getUserId());
                    return new UserNotFoundException("User not found");
                });
    }

    private static void isUsed(EmailVerificationToken verificationToken) {
        if (verificationToken.isUsed()) {
            log.warn("Verification token already used for user ID: {}", verificationToken.getUserId());
            throw new VerificationTokenInvalidException("Verification token has already been used");
        }
    }

    private static void isExpired(EmailVerificationToken verificationToken) {
        if (verificationToken.isExpired()) {
            log.warn("Verification token expired for user ID: {}", verificationToken.getUserId());
            throw new VerificationTokenExpiredException("Verification token has expired. Please request a new one");
        }
    }

    private int getExpirationDays() {
        return bookoraProperties.getVerification().getToken().getExpirationDays();
    }
}
