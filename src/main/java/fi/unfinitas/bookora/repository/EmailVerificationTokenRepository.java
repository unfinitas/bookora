package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(UUID token);

    Optional<EmailVerificationToken> findByUserIdAndUsedAtIsNull(UUID userId);

    void deleteByUserId(UUID userId);
}
