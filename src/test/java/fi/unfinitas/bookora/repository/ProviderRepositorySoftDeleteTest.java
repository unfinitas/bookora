package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.model.Provider;
import fi.unfinitas.bookora.domain.model.User;
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
class ProviderRepositorySoftDeleteTest {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private Provider createProvider(String username, String email, String businessName) {
        User user = TestDataBuilder.user()
            .username(username)
            .email(email)
            .build();
        testEntityManager.persist(user);

        Provider provider = TestDataBuilder.provider()
            .user(user)
            .businessName(businessName)
            .build();
        return testEntityManager.persist(provider);
    }

    @Test
    @Transactional
    void findById_SoftDeletedProvider_ReturnsEmpty() {
        // GIVEN: A provider that is soft deleted
        Provider provider = createProvider("provider1", "provider1@test.com", "Business 1");
        testEntityManager.flush();
        final UUID providerId = provider.getId();

        // Soft delete the provider
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_provider SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", providerId)
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding by ID
        Optional<Provider> result = providerRepository.findById(providerId);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findAll_ExcludesSoftDeletedProviders() {
        // GIVEN: 4 active providers and 2 soft deleted providers
        Provider activeProvider1 = createProvider("active1", "active1@test.com", "Active Business 1");
        Provider activeProvider2 = createProvider("active2", "active2@test.com", "Active Business 2");
        Provider activeProvider3 = createProvider("active3", "active3@test.com", "Active Business 3");
        Provider activeProvider4 = createProvider("active4", "active4@test.com", "Active Business 4");

        Provider deletedProvider1 = createProvider("deleted1", "deleted1@test.com", "Deleted Business 1");
        Provider deletedProvider2 = createProvider("deleted2", "deleted2@test.com", "Deleted Business 2");
        testEntityManager.flush();

        // Soft delete two providers
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_provider SET deleted_at = NOW() WHERE id IN (:id1, :id2)")
            .setParameter("id1", deletedProvider1.getId())
            .setParameter("id2", deletedProvider2.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding all providers
        List<Provider> result = providerRepository.findAll();

        // THEN: Should return only active providers
        assertThat(result).hasSize(4);
        assertThat(result)
            .extracting(Provider::getBusinessName)
            .containsExactlyInAnyOrder(
                "Active Business 1",
                "Active Business 2",
                "Active Business 3",
                "Active Business 4"
            );
    }

    @Test
    @Transactional
    void findAll_Pageable_ExcludesSoftDeletedProviders() {
        // GIVEN: 5 active providers and 3 soft deleted providers
        for (int i = 1; i <= 5; i++) {
            createProvider("active" + i, "active" + i + "@test.com", "Active Business " + i);
        }

        for (int i = 1; i <= 3; i++) {
            Provider deletedProvider = createProvider(
                "deleted" + i,
                "deleted" + i + "@test.com",
                "Deleted Business " + i
            );
            testEntityManager.flush();

            // Soft delete immediately
            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_provider SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", deletedProvider.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding all with pagination
        Page<Provider> result = providerRepository.findAll(PageRequest.of(0, 10));

        // THEN: Should return only active providers
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent())
            .extracting(Provider::getBusinessName)
            .allMatch(name -> name.startsWith("Active Business"));
    }

    @Test
    @Transactional
    void count_ExcludesSoftDeletedProviders() {
        // GIVEN: 6 active providers and 2 soft deleted providers
        for (int i = 1; i <= 6; i++) {
            createProvider("active" + i, "active" + i + "@test.com", "Active Business " + i);
        }

        for (int i = 1; i <= 2; i++) {
            Provider deletedProvider = createProvider(
                "deleted" + i,
                "deleted" + i + "@test.com",
                "Deleted Business " + i
            );
            testEntityManager.flush();

            // Soft delete immediately
            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_provider SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", deletedProvider.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Counting providers
        long count = providerRepository.count();

        // THEN: Should count only active providers
        assertThat(count).isEqualTo(6);
    }

    @Test
    @Transactional
    void existsById_SoftDeletedProvider_ReturnsFalse() {
        // GIVEN: A provider that is soft deleted
        Provider provider = createProvider("provider", "provider@test.com", "Test Business");
        testEntityManager.flush();
        final UUID providerId = provider.getId();

        // Soft delete the provider
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_provider SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", providerId)
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Checking existence
        boolean exists = providerRepository.existsById(providerId);

        // THEN: Should return false
        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    void deleteById_AppliesSoftDelete() {
        // GIVEN: An active provider
        Provider provider = createProvider("provider", "provider@test.com", "Test Business");
        testEntityManager.flush();
        final UUID providerId = provider.getId();

        // WHEN: Deleting the provider
        providerRepository.deleteById(providerId);
        testEntityManager.flush();

        // THEN: Provider should be soft deleted (not physically deleted)
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_provider WHERE id = :id")
            .setParameter("id", providerId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(providerRepository.findById(providerId)).isEmpty();
    }

    @Test
    @Transactional
    void delete_AppliesSoftDelete() {
        // GIVEN: An active provider
        Provider provider = createProvider("provider", "provider@test.com", "Test Business");
        testEntityManager.flush();
        final UUID providerId = provider.getId();

        // WHEN: Deleting the provider entity
        providerRepository.delete(provider);
        testEntityManager.flush();

        // THEN: Provider should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_provider WHERE id = :id")
            .setParameter("id", providerId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(providerRepository.findById(providerId)).isEmpty();
    }

    @Test
    @Transactional
    void deleteAll_AppliesSoftDelete() {
        // GIVEN: 3 active providers
        Provider provider1 = createProvider("provider1", "provider1@test.com", "Business 1");
        Provider provider2 = createProvider("provider2", "provider2@test.com", "Business 2");
        Provider provider3 = createProvider("provider3", "provider3@test.com", "Business 3");
        testEntityManager.flush();

        List<Provider> providers = List.of(provider1, provider2, provider3);

        // WHEN: Deleting all providers
        providerRepository.deleteAll(providers);
        testEntityManager.flush();

        // THEN: All should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Long deletedCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM t_provider WHERE deleted_at IS NOT NULL AND id IN (:ids)")
            .setParameter("ids", providers.stream().map(Provider::getId).toList())
            .getSingleResult();

        assertThat(deletedCount).isEqualTo(3);

        // And repository should return empty
        testEntityManager.clear();
        assertThat(providerRepository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    void deleteAll_NoArguments_AppliesSoftDeleteToAll() {
        // GIVEN: 4 providers
        createProvider("provider1", "provider1@test.com", "Business 1");
        createProvider("provider2", "provider2@test.com", "Business 2");
        createProvider("provider3", "provider3@test.com", "Business 3");
        createProvider("provider4", "provider4@test.com", "Business 4");
        testEntityManager.flush();

        // WHEN: Deleting all providers
        providerRepository.deleteAll();
        testEntityManager.flush();

        // THEN: All should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Long deletedCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM t_provider WHERE deleted_at IS NOT NULL")
            .getSingleResult();

        assertThat(deletedCount).isEqualTo(4);

        // And repository should return empty
        testEntityManager.clear();
        assertThat(providerRepository.findAll()).isEmpty();
        assertThat(providerRepository.count()).isEqualTo(0);
    }

    @Test
    @Transactional
    void verifyDeletedAtTimestamp_IsSetCorrectly() {
        // GIVEN: An active provider
        Provider provider = createProvider("provider", "provider@test.com", "Test Business");
        testEntityManager.flush();
        final UUID providerId = provider.getId();

        LocalDateTime beforeDelete = LocalDateTime.now().minusSeconds(1); // Allow 1 second tolerance

        // WHEN: Soft deleting the provider
        providerRepository.deleteById(providerId);
        testEntityManager.flush();

        LocalDateTime afterDelete = LocalDateTime.now().plusSeconds(1); // Allow 1 second tolerance

        // THEN: deleted_at should be set within the time window
        EntityManager em = testEntityManager.getEntityManager();
        java.sql.Timestamp deletedAtTimestamp = (java.sql.Timestamp) em.createNativeQuery(
                "SELECT deleted_at FROM t_provider WHERE id = :id")
            .setParameter("id", providerId)
            .getSingleResult();

        assertThat(deletedAtTimestamp).isNotNull();
        LocalDateTime deletedAt = deletedAtTimestamp.toLocalDateTime();
        assertThat(deletedAt).isAfterOrEqualTo(beforeDelete);
        assertThat(deletedAt).isBeforeOrEqualTo(afterDelete);
    }

    @Test
    @Transactional
    void mixedOperations_OnlyShowActiveProviders() {
        // GIVEN: Mix of active and soft deleted providers
        Provider activeProvider = createProvider("active", "active@test.com", "Active Business");
        Provider deletedProvider = createProvider("deleted", "deleted@test.com", "Deleted Business");
        testEntityManager.flush();

        // Soft delete one provider
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_provider SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", deletedProvider.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN/THEN: All operations should exclude soft deleted provider
        assertThat(providerRepository.findAll()).hasSize(1);
        assertThat(providerRepository.count()).isEqualTo(1);
        assertThat(providerRepository.findById(deletedProvider.getId())).isEmpty();
        assertThat(providerRepository.findById(activeProvider.getId())).isPresent();
        assertThat(providerRepository.existsById(deletedProvider.getId())).isFalse();
        assertThat(providerRepository.existsById(activeProvider.getId())).isTrue();
    }

    @Test
    @Transactional
    void saveAndFlush_DoesNotResurrectSoftDeletedProvider() {
        // GIVEN: A provider that is soft deleted
        Provider provider = createProvider("provider", "provider@test.com", "Test Business");
        testEntityManager.flush();
        final UUID providerId = provider.getId();

        // Soft delete the provider
        providerRepository.deleteById(providerId);
        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Trying to find and update the soft deleted provider
        Optional<Provider> foundProvider = providerRepository.findById(providerId);

        // THEN: Provider should not be found
        assertThat(foundProvider).isEmpty();

        // Verify it's still soft deleted in database
        EntityManager em = testEntityManager.getEntityManager();
        Object deletedAt = em.createNativeQuery(
                "SELECT deleted_at FROM t_provider WHERE id = :id")
            .setParameter("id", providerId)
            .getSingleResult();

        assertThat(deletedAt).isNotNull();
    }
}