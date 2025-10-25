package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.testutil.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RepositoryTestConfiguration.class)
@ActiveProfiles("test")
class EmailVerificationTokenRepositoryTest {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByToken_ExistingToken_ReturnsToken() {
        // GIVEN: Token exists in database
        final User user = TestDataBuilder.user()
                .isEmailVerified(false)
                .build();
        entityManager.persist(user);

        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        final EmailVerificationToken saved = entityManager.persistAndFlush(token);

        // WHEN: findByToken is called
        final Optional<EmailVerificationToken> result = tokenRepository.findByToken(saved.getToken());

        // THEN: Returns Optional<EmailVerificationToken>
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo(saved.getToken());
        assertThat(result.get().getUserId()).isEqualTo(user.getId());
    }

    @Test
    void findByToken_NonExistentToken_ReturnsEmpty() {
        // GIVEN: Token does not exist
        final UUID nonExistentToken = UUID.randomUUID();

        // WHEN: findByToken is called
        final Optional<EmailVerificationToken> result = tokenRepository.findByToken(nonExistentToken);

        // THEN: Returns Optional.empty()
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndUsedAtIsNull_ExistingUnusedToken_ReturnsToken() {
        // GIVEN: User has an unused token
        final User user = TestDataBuilder.user()
                .isEmailVerified(false)
                .build();
        entityManager.persist(user);

        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(null)
                .build();
        entityManager.persistAndFlush(token);

        // WHEN: findByUserIdAndUsedAtIsNull is called
        final Optional<EmailVerificationToken> result = tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());

        // THEN: Returns Optional<EmailVerificationToken>
        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(user.getId());
        assertThat(result.get().getUsedAt()).isNull();
    }

    @Test
    void findByUserIdAndUsedAtIsNull_OnlyUsedTokenExists_ReturnsEmpty() {
        // GIVEN: User has only a used token
        final User user = TestDataBuilder.user()
                .isEmailVerified(true)
                .build();
        entityManager.persist(user);

        final EmailVerificationToken usedToken = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(usedToken);

        // WHEN: findByUserIdAndUsedAtIsNull is called
        final Optional<EmailVerificationToken> result = tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());

        // THEN: Returns Optional.empty()
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndUsedAtIsNull_MultipleTokens_ReturnsUnusedToken() {
        // GIVEN: User has both used and unused tokens
        final User user = TestDataBuilder.user()
                .isEmailVerified(false)
                .build();
        entityManager.persist(user);

        final EmailVerificationToken usedToken = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(LocalDateTime.now().minusDays(1))
                .build();
        entityManager.persist(usedToken);

        final EmailVerificationToken unusedToken = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(null)
                .build();
        entityManager.persistAndFlush(unusedToken);

        // WHEN: findByUserIdAndUsedAtIsNull is called
        final Optional<EmailVerificationToken> result = tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());

        // THEN: Returns the unused token
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo(unusedToken.getToken());
        assertThat(result.get().getUsedAt()).isNull();
    }

    @Test
    void findByUserIdAndUsedAtIsNull_NoToken_ReturnsEmpty() {
        // GIVEN: User has no tokens
        final User user = TestDataBuilder.user()
                .isEmailVerified(false)
                .build();
        entityManager.persistAndFlush(user);

        // WHEN: findByUserIdAndUsedAtIsNull is called
        final Optional<EmailVerificationToken> result = tokenRepository.findByUserIdAndUsedAtIsNull(user.getId());

        // THEN: Returns Optional.empty()
        assertThat(result).isEmpty();
    }

    @Test
    void deleteByUserId_TokensExist_DeletesAllTokens() {
        // GIVEN: User has multiple tokens
        final User user = TestDataBuilder.user()
                .isEmailVerified(false)
                .build();
        entityManager.persist(user);

        final EmailVerificationToken token1 = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        entityManager.persist(token1);

        final EmailVerificationToken token2 = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        entityManager.persistAndFlush(token2);

        // WHEN: deleteByUserId is called
        tokenRepository.deleteByUserId(user.getId());
        entityManager.flush();

        // THEN: All tokens for user are deleted
        final Optional<EmailVerificationToken> result1 = tokenRepository.findByToken(token1.getToken());
        final Optional<EmailVerificationToken> result2 = tokenRepository.findByToken(token2.getToken());

        assertThat(result1).isEmpty();
        assertThat(result2).isEmpty();
    }

    @Test
    void deleteByUserId_NoTokens_DoesNotThrowException() {
        // GIVEN: User has no tokens
        final User user = TestDataBuilder.user()
                .isEmailVerified(false)
                .build();
        entityManager.persistAndFlush(user);

        // WHEN: deleteByUserId is called
        // THEN: No exception is thrown
        tokenRepository.deleteByUserId(user.getId());
        entityManager.flush();
    }

    @Test
    void deleteByUserId_OnlyDeletesSpecifiedUserTokens() {
        // GIVEN: Two users with tokens
        final User user1 = TestDataBuilder.user()
                .email("user1@example.com")
                .username("user1")
                .isEmailVerified(false)
                .build();
        entityManager.persist(user1);

        final User user2 = TestDataBuilder.user()
                .email("user2@example.com")
                .username("user2")
                .isEmailVerified(false)
                .build();
        entityManager.persist(user2);

        final EmailVerificationToken token1 = EmailVerificationToken.builder()
                .userId(user1.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        entityManager.persist(token1);

        final EmailVerificationToken token2 = EmailVerificationToken.builder()
                .userId(user2.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        entityManager.persistAndFlush(token2);

        // WHEN: deleteByUserId is called for user1
        tokenRepository.deleteByUserId(user1.getId());
        entityManager.flush();

        // THEN: Only user1's token is deleted, user2's token remains
        final Optional<EmailVerificationToken> result1 = tokenRepository.findByToken(token1.getToken());
        final Optional<EmailVerificationToken> result2 = tokenRepository.findByToken(token2.getToken());

        assertThat(result1).isEmpty();
        assertThat(result2).isPresent();
    }

    @Test
    void save_NewToken_SavesSuccessfully() {
        // GIVEN: New EmailVerificationToken
        final User user = TestDataBuilder.user()
                .isEmailVerified(false)
                .build();
        entityManager.persist(user);

        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        // WHEN: save is called
        final EmailVerificationToken saved = tokenRepository.save(token);
        entityManager.flush();

        // THEN: Token is persisted with ID
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getToken()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(user.getId());
    }

    @Test
    void save_UpdateToken_UpdatesSuccessfully() {
        // GIVEN: Existing token
        final User user = TestDataBuilder.user()
                .isEmailVerified(false)
                .build();
        entityManager.persist(user);

        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(user.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .usedAt(null)
                .build();
        final EmailVerificationToken saved = entityManager.persistAndFlush(token);

        // WHEN: Token is marked as used and saved
        saved.markAsUsed();
        final EmailVerificationToken updated = tokenRepository.save(saved);
        entityManager.flush();

        // THEN: usedAt is persisted
        final Optional<EmailVerificationToken> result = tokenRepository.findByToken(saved.getToken());
        assertThat(result).isPresent();
        assertThat(result.get().getUsedAt()).isNotNull();
    }
}
