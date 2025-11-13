package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.model.RefreshToken;
import fi.unfinitas.bookora.exception.InvalidTokenException;
import fi.unfinitas.bookora.exception.TokenExpiredException;
import fi.unfinitas.bookora.exception.TokenReuseDetectedException;
import fi.unfinitas.bookora.repository.RefreshTokenRepository;
import fi.unfinitas.bookora.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private BookoraProperties bookoraProperties;

    @Mock
    private BookoraProperties.Jwt jwtProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private UUID userId;
    private UUID tokenFamily;
    private RefreshToken mockToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tokenFamily = UUID.randomUUID();

        lenient().when(bookoraProperties.getJwt()).thenReturn(jwtProperties);
        lenient().when(jwtProperties.getRefreshTokenExpirationSeconds()).thenReturn(604800L); // 7 days

        mockToken = RefreshToken.builder()
                .id(1L)
                .userId(userId)
                .tokenHash("hash123")
                .tokenFamily(tokenFamily)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void shouldCreateRefreshTokenSuccessfully() {
        // Given
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setRawToken("generated-token");
            return token;
        });

        // When
        RefreshToken result = refreshTokenService.createRefreshToken(userId, tokenFamily);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTokenFamily()).isEqualTo(tokenFamily);
        assertThat(result.getRawToken()).isNotNull();

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldCreateRefreshTokenWithNewFamilyWhenNullProvided() {
        // Given
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setRawToken("generated-token");
            return token;
        });

        // When
        RefreshToken result = refreshTokenService.createRefreshToken(userId, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTokenFamily()).isNotNull();

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldValidateAndRotateTokenSuccessfully() {
        // Given
        String rawToken = "valid-token";
        mockToken.setRawToken(rawToken);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(mockToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getRawToken() == null) {
                token.setRawToken("new-token");
            }
            return token;
        });

        // When
        RefreshToken result = refreshTokenService.validateAndRotateToken(rawToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTokenFamily()).isEqualTo(tokenFamily);
        assertThat(result.getRawToken()).isNotNull();

        // Verify old token was revoked
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        assertThat(mockToken.isRevoked()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenTokenNotFound() {
        // Given
        String rawToken = "non-existent-token";
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.validateAndRotateToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid refresh token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenTokenAlreadyRevoked() {
        // Given
        String rawToken = "revoked-token";
        mockToken.revoke();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(mockToken));

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.validateAndRotateToken(rawToken))
                .isInstanceOf(TokenReuseDetectedException.class)
                .hasMessage("Token reuse detected. All tokens in family have been revoked.");

        // Verify entire token family was revoked
        verify(refreshTokenRepository).revokeAllByTokenFamily(eq(tokenFamily), any(LocalDateTime.class));
    }

    @Test
    void shouldThrowExceptionWhenTokenExpired() {
        // Given
        String rawToken = "expired-token";
        mockToken = RefreshToken.builder()
                .id(1L)
                .userId(userId)
                .tokenHash("hash123")
                .tokenFamily(tokenFamily)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(mockToken));

        // When & Then
        assertThatThrownBy(() -> refreshTokenService.validateAndRotateToken(rawToken))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessage("Refresh token has expired");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRevokeTokenSuccessfully() {
        // Given
        String rawToken = "token-to-revoke";
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(mockToken));

        // When
        refreshTokenService.revokeToken(rawToken);

        // Then
        assertThat(mockToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(mockToken);
    }

    @Test
    void shouldNotRevokeAlreadyRevokedToken() {
        // Given
        String rawToken = "already-revoked-token";
        mockToken.revoke();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(mockToken));

        // When
        refreshTokenService.revokeToken(rawToken);

        // Then
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRevokeAllUserTokens() {
        // Given
        when(refreshTokenRepository.revokeAllByUserId(eq(userId), any(LocalDateTime.class))).thenReturn(5);

        // When
        refreshTokenService.revokeAllUserTokens(userId);

        // Then
        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void shouldRevokeEntireTokenFamily() {
        // Given
        when(refreshTokenRepository.revokeAllByTokenFamily(eq(tokenFamily), any(LocalDateTime.class))).thenReturn(3);

        // When
        refreshTokenService.revokeTokenFamily(tokenFamily);

        // Then
        verify(refreshTokenRepository).revokeAllByTokenFamily(eq(tokenFamily), any(LocalDateTime.class));
    }

    @Test
    void shouldCleanupExpiredTokens() {
        // Given
        when(refreshTokenRepository.deleteExpiredTokens(any(LocalDateTime.class))).thenReturn(10);

        // When
        refreshTokenService.cleanupExpiredTokens();

        // Then
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).deleteExpiredTokens(captor.capture());

        // Verify cutoff date is approximately 30 days ago
        LocalDateTime cutoffDate = captor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(30);
        assertThat(cutoffDate).isBetween(expectedCutoff.minusMinutes(1), expectedCutoff.plusMinutes(1));
    }

    @Test
    void shouldGetActiveTokenCountForUser() {
        // Given
        when(refreshTokenRepository.countActiveTokensByUserId(eq(userId), any(LocalDateTime.class))).thenReturn(3L);

        // When
        long count = refreshTokenService.getActiveTokenCount(userId);

        // Then
        assertThat(count).isEqualTo(3L);
        verify(refreshTokenRepository).countActiveTokensByUserId(eq(userId), any(LocalDateTime.class));
    }
}