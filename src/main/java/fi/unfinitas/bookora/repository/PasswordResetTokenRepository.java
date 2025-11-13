package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a password reset token by its token UUID.
     *
     * @param token the token UUID
     * @return Optional containing the token if found
     */
    Optional<PasswordResetToken> findByToken(UUID token);

    /**
     * Find a valid (unused) password reset token for a user.
     * Used to prevent multiple active tokens for the same user.
     *
     * @param userId the user ID
     * @return Optional containing the unused token if found
     */
    Optional<PasswordResetToken> findByUserIdAndUsedAtIsNull(UUID userId);

    /**
     * Delete all password reset tokens for a specific user.
     * Used when generating a new token to invalidate old ones.
     *
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);
}
