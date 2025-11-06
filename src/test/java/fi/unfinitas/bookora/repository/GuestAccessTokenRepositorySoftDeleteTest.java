package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.model.*;
import fi.unfinitas.bookora.testutil.TestDataBuilder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RepositoryTestConfiguration.class)
@ActiveProfiles("test")
class GuestAccessTokenRepositorySoftDeleteTest {

    @Autowired
    private GuestAccessTokenRepository guestAccessTokenRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private Booking createBooking(String customerUsername, String customerEmail) {
        // Create customer
        User customer = TestDataBuilder.guestUser()
            .username(customerUsername)
            .email(customerEmail)
            .build();
        testEntityManager.persist(customer);

        // Create provider
        User providerUser = TestDataBuilder.user()
            .username("provider-" + customerUsername)
            .email("provider-" + customerEmail)
            .build();
        testEntityManager.persist(providerUser);

        Provider provider = TestDataBuilder.provider()
            .user(providerUser)
            .businessName("Business for " + customerUsername)
            .build();
        testEntityManager.persist(provider);

        // Create service
        ServiceOffering service = TestDataBuilder.serviceOffering()
            .provider(provider)
            .build();
        testEntityManager.persist(service);

        // Create booking
        Booking booking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(service)
            .build();
        return testEntityManager.persist(booking);
    }

    private GuestAccessToken createToken(Booking booking) {
        GuestAccessToken token = TestDataBuilder.guestAccessToken()
            .booking(booking)
            .token(UUID.randomUUID())
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        return testEntityManager.persist(token);
    }

    @Test
    @Transactional
    void findById_SoftDeletedToken_ReturnsEmpty() {
        // GIVEN: A token that is soft deleted
        Booking booking = createBooking("customer1", "customer1@test.com");
        GuestAccessToken token = createToken(booking);
        testEntityManager.flush();
        final Long tokenId = token.getId();

        // Soft delete the token
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", tokenId)
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding by ID
        Optional<GuestAccessToken> result = guestAccessTokenRepository.findById(tokenId);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findByToken_SoftDeletedToken_ReturnsEmpty() {
        // GIVEN: A token that is soft deleted
        Booking booking = createBooking("customer", "customer@test.com");
        final UUID tokenValue = UUID.randomUUID();
        GuestAccessToken token = TestDataBuilder.guestAccessToken()
            .booking(booking)
            .token(tokenValue)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        token = testEntityManager.persist(token);
        testEntityManager.flush();

        // Soft delete the token
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", token.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding by token value
        Optional<GuestAccessToken> result = guestAccessTokenRepository.findByToken(tokenValue);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findByBookingId_SoftDeletedToken_ReturnsEmpty() {
        // GIVEN: A token that is soft deleted
        Booking booking = createBooking("customer", "customer@test.com");
        final Long bookingId = booking.getId();
        GuestAccessToken token = createToken(booking);
        testEntityManager.flush();

        // Soft delete the token
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", token.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding by booking ID
        Optional<GuestAccessToken> result = guestAccessTokenRepository.findByBookingId(bookingId);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findAll_ExcludesSoftDeletedTokens() {
        // GIVEN: 4 active tokens and 2 soft deleted tokens
        // Create active tokens
        for (int i = 1; i <= 4; i++) {
            Booking booking = createBooking("active" + i, "active" + i + "@test.com");
            createToken(booking);
        }

        // Create and soft delete tokens
        for (int i = 1; i <= 2; i++) {
            Booking booking = createBooking("deleted" + i, "deleted" + i + "@test.com");
            GuestAccessToken toDelete = createToken(booking);
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding all tokens
        List<GuestAccessToken> result = guestAccessTokenRepository.findAll();

        // THEN: Should return only active tokens
        assertThat(result).hasSize(4);
    }

    @Test
    @Transactional
    void findAll_Pageable_ExcludesSoftDeletedTokens() {
        // GIVEN: 5 active tokens and 3 soft deleted tokens
        for (int i = 1; i <= 5; i++) {
            Booking booking = createBooking("active" + i, "active" + i + "@test.com");
            createToken(booking);
        }

        for (int i = 1; i <= 3; i++) {
            Booking booking = createBooking("deleted" + i, "deleted" + i + "@test.com");
            GuestAccessToken toDelete = createToken(booking);
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding with pagination
        Page<GuestAccessToken> result = guestAccessTokenRepository.findAll(PageRequest.of(0, 10));

        // THEN: Should return only active tokens
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(5);
    }

    @Test
    @Transactional
    void count_ExcludesSoftDeletedTokens() {
        // GIVEN: 6 active tokens and 2 soft deleted tokens
        for (int i = 1; i <= 6; i++) {
            Booking booking = createBooking("active" + i, "active" + i + "@test.com");
            createToken(booking);
        }

        for (int i = 1; i <= 2; i++) {
            Booking booking = createBooking("deleted" + i, "deleted" + i + "@test.com");
            GuestAccessToken toDelete = createToken(booking);
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Counting tokens
        long count = guestAccessTokenRepository.count();

        // THEN: Should count only active tokens
        assertThat(count).isEqualTo(6);
    }

    @Test
    @Transactional
    void existsById_SoftDeletedToken_ReturnsFalse() {
        // GIVEN: A token that is soft deleted
        Booking booking = createBooking("customer", "customer@test.com");
        GuestAccessToken token = createToken(booking);
        testEntityManager.flush();
        final Long tokenId = token.getId();

        // Soft delete the token
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", tokenId)
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Checking existence
        boolean exists = guestAccessTokenRepository.existsById(tokenId);

        // THEN: Should return false
        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    void deleteById_AppliesSoftDelete() {
        // GIVEN: An active token
        Booking booking = createBooking("customer", "customer@test.com");
        GuestAccessToken token = createToken(booking);
        testEntityManager.flush();
        final Long tokenId = token.getId();

        // WHEN: Deleting the token
        guestAccessTokenRepository.deleteById(tokenId);
        testEntityManager.flush();

        // THEN: Token should be soft deleted (not physically deleted)
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_guest_access_token WHERE id = :id")
            .setParameter("id", tokenId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(guestAccessTokenRepository.findById(tokenId)).isEmpty();
    }

    @Test
    @Transactional
    void delete_AppliesSoftDelete() {
        // GIVEN: An active token
        Booking booking = createBooking("customer", "customer@test.com");
        GuestAccessToken token = createToken(booking);
        testEntityManager.flush();
        final Long tokenId = token.getId();

        // WHEN: Deleting the token entity
        guestAccessTokenRepository.delete(token);
        testEntityManager.flush();

        // THEN: Token should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_guest_access_token WHERE id = :id")
            .setParameter("id", tokenId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(guestAccessTokenRepository.findById(tokenId)).isEmpty();
    }

    @Test
    @Transactional
    void deleteAll_AppliesSoftDelete() {
        // GIVEN: 3 active tokens
        GuestAccessToken token1 = createToken(createBooking("customer1", "customer1@test.com"));
        GuestAccessToken token2 = createToken(createBooking("customer2", "customer2@test.com"));
        GuestAccessToken token3 = createToken(createBooking("customer3", "customer3@test.com"));
        testEntityManager.flush();

        List<GuestAccessToken> tokens = List.of(token1, token2, token3);

        // WHEN: Deleting all tokens
        guestAccessTokenRepository.deleteAll(tokens);
        testEntityManager.flush();

        // THEN: All should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Long deletedCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM t_guest_access_token WHERE deleted_at IS NOT NULL AND id IN (:ids)")
            .setParameter("ids", tokens.stream().map(GuestAccessToken::getId).toList())
            .getSingleResult();

        assertThat(deletedCount).isEqualTo(3);

        // And repository should return empty
        testEntityManager.clear();
        assertThat(guestAccessTokenRepository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    void verifyDeletedAtTimestamp_IsSetCorrectly() {
        // GIVEN: An active token
        Booking booking = createBooking("customer", "customer@test.com");
        GuestAccessToken token = createToken(booking);
        testEntityManager.flush();
        final Long tokenId = token.getId();

        LocalDateTime beforeDelete = LocalDateTime.now().minusSeconds(1); // Allow 1 second tolerance

        // WHEN: Soft deleting the token
        guestAccessTokenRepository.deleteById(tokenId);
        testEntityManager.flush();

        LocalDateTime afterDelete = LocalDateTime.now().plusSeconds(1); // Allow 1 second tolerance

        // THEN: deleted_at should be set within the time window
        EntityManager em = testEntityManager.getEntityManager();
        java.sql.Timestamp deletedAtTimestamp = (java.sql.Timestamp) em.createNativeQuery(
                "SELECT deleted_at FROM t_guest_access_token WHERE id = :id")
            .setParameter("id", tokenId)
            .getSingleResult();

        assertThat(deletedAtTimestamp).isNotNull();
        LocalDateTime deletedAt = deletedAtTimestamp.toLocalDateTime();
        assertThat(deletedAt).isAfterOrEqualTo(beforeDelete);
        assertThat(deletedAt).isBeforeOrEqualTo(afterDelete);
    }

    @Test
    @Transactional
    void mixedOperations_OnlyShowActiveTokens() {
        // GIVEN: Mix of active and soft deleted tokens
        Booking activeBooking = createBooking("active", "active@test.com");
        Booking deletedBooking = createBooking("deleted", "deleted@test.com");

        final UUID activeTokenValue = UUID.randomUUID();
        final UUID deletedTokenValue = UUID.randomUUID();

        GuestAccessToken activeToken = TestDataBuilder.guestAccessToken()
            .booking(activeBooking)
            .token(activeTokenValue)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        activeToken = testEntityManager.persist(activeToken);

        GuestAccessToken deletedToken = TestDataBuilder.guestAccessToken()
            .booking(deletedBooking)
            .token(deletedTokenValue)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .build();
        deletedToken = testEntityManager.persist(deletedToken);
        testEntityManager.flush();

        // Soft delete one token
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", deletedToken.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN/THEN: All operations should exclude soft deleted token
        assertThat(guestAccessTokenRepository.findAll()).hasSize(1);
        assertThat(guestAccessTokenRepository.count()).isEqualTo(1);
        assertThat(guestAccessTokenRepository.findByToken(deletedTokenValue)).isEmpty();
        assertThat(guestAccessTokenRepository.findByToken(activeTokenValue)).isPresent();
        assertThat(guestAccessTokenRepository.findByBookingId(deletedBooking.getId())).isEmpty();
        assertThat(guestAccessTokenRepository.findByBookingId(activeBooking.getId())).isPresent();
        assertThat(guestAccessTokenRepository.existsById(deletedToken.getId())).isFalse();
        assertThat(guestAccessTokenRepository.existsById(activeToken.getId())).isTrue();
    }

    @Test
    @Transactional
    void expiredToken_NotSoftDeleted_StillReturned() {
        // GIVEN: An expired but not soft deleted token
        Booking booking = createBooking("customer", "customer@test.com");
        final UUID tokenValue = UUID.randomUUID();
        GuestAccessToken expiredToken = TestDataBuilder.guestAccessToken()
            .booking(booking)
            .token(tokenValue)
            .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
            .build();
        expiredToken = testEntityManager.persistAndFlush(expiredToken);

        testEntityManager.clear();

        // WHEN: Finding the expired token
        Optional<GuestAccessToken> result = guestAccessTokenRepository.findByToken(tokenValue);

        // THEN: Should still return the expired token (not soft deleted)
        assertThat(result).isPresent();
        assertThat(result.get().isExpired()).isTrue();
    }

    @Test
    @Transactional
    void confirmedToken_SoftDeleted_NotReturned() {
        // GIVEN: A confirmed token that is soft deleted
        Booking booking = createBooking("customer", "customer@test.com");
        final UUID tokenValue = UUID.randomUUID();
        GuestAccessToken token = TestDataBuilder.guestAccessToken()
            .booking(booking)
            .token(tokenValue)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .confirmedAt(LocalDateTime.now().minusHours(1))
            .build();
        token = testEntityManager.persist(token);
        testEntityManager.flush();

        // Soft delete the confirmed token
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_guest_access_token SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", token.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding the soft deleted confirmed token
        Optional<GuestAccessToken> result = guestAccessTokenRepository.findByToken(tokenValue);

        // THEN: Should not return the token (soft deleted)
        assertThat(result).isEmpty();
    }
}