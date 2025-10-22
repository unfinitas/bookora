package fi.unfinitas.bookora.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTokenTest {

    @Test
    @DisplayName("Should return true when token is expired")
    void shouldReturnTrueWhenTokenIsExpired() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("Should return false when token is not expired")
    void shouldReturnFalseWhenTokenIsNotExpired() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Should return true when token expiration is exactly now")
    void shouldReturnTrueWhenTokenExpirationIsExactlyNow() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().minusNanos(1))
                .build();

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("Should return true when token is used")
    void shouldReturnTrueWhenTokenIsUsed() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(LocalDateTime.now())
                .build();

        assertThat(token.isUsed()).isTrue();
    }

    @Test
    @DisplayName("Should return false when token is not used")
    void shouldReturnFalseWhenTokenIsNotUsed() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(null)
                .build();

        assertThat(token.isUsed()).isFalse();
    }

    @Test
    @DisplayName("Should return true when token is valid (not expired and not used)")
    void shouldReturnTrueWhenTokenIsValid() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(null)
                .build();

        assertThat(token.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should return false when token is expired even if not used")
    void shouldReturnFalseWhenTokenIsExpiredEvenIfNotUsed() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().minusDays(1))
                .usedAt(null)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should return false when token is used even if not expired")
    void shouldReturnFalseWhenTokenIsUsedEvenIfNotExpired() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(LocalDateTime.now())
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should return false when token is both expired and used")
    void shouldReturnFalseWhenTokenIsBothExpiredAndUsed() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().minusDays(1))
                .usedAt(LocalDateTime.now())
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should mark token as used with current timestamp")
    void shouldMarkTokenAsUsedWithCurrentTimestamp() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(null)
                .build();

        assertThat(token.getUsedAt()).isNull();
        assertThat(token.isUsed()).isFalse();

        token.markAsUsed();

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(token.isUsed()).isTrue();
        assertThat(token.getUsedAt()).isBetween(
                LocalDateTime.now().minusSeconds(1),
                LocalDateTime.now().plusSeconds(1)
        );
    }

    @Test
    @DisplayName("Should invalidate token after marking as used")
    void shouldInvalidateTokenAfterMarkingAsUsed() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(null)
                .build();

        assertThat(token.isValid()).isTrue();

        token.markAsUsed();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should allow marking as used multiple times without error")
    void shouldAllowMarkingAsUsedMultipleTimesWithoutError() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(null)
                .build();

        token.markAsUsed();
        final LocalDateTime firstUsedAt = token.getUsedAt();

        token.markAsUsed();
        final LocalDateTime secondUsedAt = token.getUsedAt();

        assertThat(secondUsedAt).isAfterOrEqualTo(firstUsedAt);
        assertThat(token.isUsed()).isTrue();
    }
}
