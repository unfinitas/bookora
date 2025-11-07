package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.model.RefreshToken;

import java.util.UUID;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(UUID userId, UUID tokenFamily);

    RefreshToken validateAndRotateToken(String rawToken);

    void revokeToken(String rawToken);

    void revokeAllUserTokens(UUID userId);

    void revokeTokenFamily(UUID tokenFamily);

    void cleanupExpiredTokens();

    long getActiveTokenCount(UUID userId);
}