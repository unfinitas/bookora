package fi.unfinitas.bookora.service.impl;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.model.RefreshToken;
import fi.unfinitas.bookora.exception.InvalidTokenException;
import fi.unfinitas.bookora.exception.TokenExpiredException;
import fi.unfinitas.bookora.exception.TokenReuseDetectedException;
import fi.unfinitas.bookora.repository.RefreshTokenRepository;
import fi.unfinitas.bookora.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final BookoraProperties bookoraProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public RefreshToken createRefreshToken(UUID userId, UUID tokenFamily) {
        // Generate secure random token (256 bits)
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash the token for storage
        String tokenHash = hashToken(rawToken);

        // Create and save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .tokenFamily(tokenFamily != null ? tokenFamily : UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusSeconds(bookoraProperties.getJwt().getRefreshTokenExpirationSeconds()))
                .build();


        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        saved.setRawToken(rawToken);  // Set raw token for return (not stored in DB)

        log.debug("Created new refresh token for user: {}, family: {}", userId, refreshToken.getTokenFamily());

        return saved;
    }

    @Override
    public RefreshToken validateAndRotateToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Check if token was already used (revoked)
        if (existingToken.isRevoked()) {
            log.warn("Token reuse detected for user: {}, family: {}",
                    existingToken.getUserId(), existingToken.getTokenFamily());

            // Token reuse detected - revoke entire family
            revokeTokenFamily(existingToken.getTokenFamily());

            throw new TokenReuseDetectedException("Token reuse detected. All tokens in family have been revoked.");
        }

        // Check if token is expired
        if (existingToken.isExpired()) {
            log.debug("Expired token used by user: {}", existingToken.getUserId());
            throw new TokenExpiredException("Refresh token has expired");
        }

        // Token is valid - create new token with same family
        RefreshToken newToken = createRefreshToken(existingToken.getUserId(), existingToken.getTokenFamily());

        // Revoke old token and link to new one
        existingToken.revokeAndReplace(newToken.getId());
        refreshTokenRepository.save(existingToken);

        log.debug("Token rotated successfully for user: {}, family: {}",
                existingToken.getUserId(), existingToken.getTokenFamily());

        return newToken;
    }

    @Override
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.revoke();
                refreshTokenRepository.save(token);
                log.debug("Token revoked for user: {}", token.getUserId());
            }
        });
    }

    @Override
    public void revokeAllUserTokens(UUID userId) {
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
        log.debug("Revoked {} tokens for user: {}", revokedCount, userId);
    }

    @Override
    public void revokeTokenFamily(UUID tokenFamily) {
        int revokedCount = refreshTokenRepository.revokeAllByTokenFamily(tokenFamily, LocalDateTime.now());
        log.warn("Revoked entire token family: {}, {} tokens affected", tokenFamily, revokedCount);
    }

    @Override
    public void cleanupExpiredTokens() {
        // Delete tokens that have been expired for more than 30 days
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(cutoffDate);
        log.info("Cleanup: Deleted {} expired refresh tokens", deletedCount);
    }

    @Override
    public long getActiveTokenCount(UUID userId) {
        return refreshTokenRepository.countActiveTokensByUserId(userId, LocalDateTime.now());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}