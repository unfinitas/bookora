package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserId(UUID userId);

    List<RefreshToken> findByTokenFamily(UUID tokenFamily);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * Revoke all tokens in a family when token reuse is detected
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.tokenFamily = :tokenFamily AND rt.revokedAt IS NULL")
    int revokeAllByTokenFamily(@Param("tokenFamily") UUID tokenFamily, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :expiredBefore")
    int deleteExpiredTokens(@Param("expiredBefore") LocalDateTime expiredBefore);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    long countActiveTokensByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.userId = :userId AND rt.revokedAt IS NULL AND rt.expiresAt > :now ORDER BY rt.createdAt DESC")
    Optional<RefreshToken> findMostRecentActiveTokenByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}