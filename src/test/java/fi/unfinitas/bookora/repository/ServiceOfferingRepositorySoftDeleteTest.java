package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.model.Provider;
import fi.unfinitas.bookora.domain.model.ServiceOffering;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RepositoryTestConfiguration.class)
@ActiveProfiles("test")
class ServiceOfferingRepositorySoftDeleteTest {

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private Provider createProvider(String username, String email) {
        User user = TestDataBuilder.user()
            .username(username)
            .email(email)
            .build();
        testEntityManager.persist(user);

        Provider provider = TestDataBuilder.provider()
            .user(user)
            .businessName("Business " + username)
            .build();
        return testEntityManager.persist(provider);
    }

    private ServiceOffering createService(Provider provider, String name, BigDecimal price) {
        ServiceOffering service = TestDataBuilder.serviceOffering()
            .provider(provider)
            .name(name)
            .price(price)
            .durationMinutes(60)
            .build();
        return testEntityManager.persist(service);
    }

    @Test
    @Transactional
    void findById_SoftDeletedService_ReturnsEmpty() {
        // GIVEN: A service that is soft deleted
        Provider provider = createProvider("provider1", "provider1@test.com");
        ServiceOffering service = createService(provider, "Service 1", BigDecimal.valueOf(100));
        testEntityManager.flush();
        final Long serviceId = service.getId();

        // Soft delete the service
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_service SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", serviceId)
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding by ID
        Optional<ServiceOffering> result = serviceOfferingRepository.findById(serviceId);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findAll_ExcludesSoftDeletedServices() {
        // GIVEN: 5 active services and 3 soft deleted services
        Provider provider = createProvider("provider", "provider@test.com");

        // Create active services
        for (int i = 1; i <= 5; i++) {
            createService(provider, "Active Service " + i, BigDecimal.valueOf(100 + i * 10));
        }

        // Create and soft delete services
        for (int i = 1; i <= 3; i++) {
            ServiceOffering toDelete = createService(
                provider,
                "Deleted Service " + i,
                BigDecimal.valueOf(200 + i * 10)
            );
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_service SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding all services
        List<ServiceOffering> result = serviceOfferingRepository.findAll();

        // THEN: Should return only active services
        assertThat(result).hasSize(5);
        assertThat(result)
            .extracting(ServiceOffering::getName)
            .allMatch(name -> name.startsWith("Active Service"));
    }

    @Test
    @Transactional
    void findAll_Pageable_ExcludesSoftDeletedServices() {
        // GIVEN: 6 active services and 4 soft deleted services
        Provider provider = createProvider("provider", "provider@test.com");

        for (int i = 1; i <= 6; i++) {
            createService(provider, "Active Service " + i, BigDecimal.valueOf(100 + i * 10));
        }

        for (int i = 1; i <= 4; i++) {
            ServiceOffering toDelete = createService(
                provider,
                "Deleted Service " + i,
                BigDecimal.valueOf(200 + i * 10)
            );
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_service SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding with pagination
        Page<ServiceOffering> result = serviceOfferingRepository.findAll(PageRequest.of(0, 10));

        // THEN: Should return only active services
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getContent()).hasSize(6);
        assertThat(result.getContent())
            .extracting(ServiceOffering::getName)
            .allMatch(name -> name.startsWith("Active Service"));
    }

    @Test
    @Transactional
    void count_ExcludesSoftDeletedServices() {
        // GIVEN: 7 active services and 3 soft deleted services
        Provider provider = createProvider("provider", "provider@test.com");

        for (int i = 1; i <= 7; i++) {
            createService(provider, "Active Service " + i, BigDecimal.valueOf(100 + i * 10));
        }

        for (int i = 1; i <= 3; i++) {
            ServiceOffering toDelete = createService(
                provider,
                "Deleted Service " + i,
                BigDecimal.valueOf(200 + i * 10)
            );
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_service SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Counting services
        long count = serviceOfferingRepository.count();

        // THEN: Should count only active services
        assertThat(count).isEqualTo(7);
    }

    @Test
    @Transactional
    void existsById_SoftDeletedService_ReturnsFalse() {
        // GIVEN: A service that is soft deleted
        Provider provider = createProvider("provider", "provider@test.com");
        ServiceOffering service = createService(provider, "Test Service", BigDecimal.valueOf(100));
        testEntityManager.flush();
        final Long serviceId = service.getId();

        // Soft delete the service
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_service SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", serviceId)
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Checking existence
        boolean exists = serviceOfferingRepository.existsById(serviceId);

        // THEN: Should return false
        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    void deleteById_AppliesSoftDelete() {
        // GIVEN: An active service
        Provider provider = createProvider("provider", "provider@test.com");
        ServiceOffering service = createService(provider, "Test Service", BigDecimal.valueOf(100));
        testEntityManager.flush();
        final Long serviceId = service.getId();

        // WHEN: Deleting the service
        serviceOfferingRepository.deleteById(serviceId);
        testEntityManager.flush();

        // THEN: Service should be soft deleted (not physically deleted)
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_service WHERE id = :id")
            .setParameter("id", serviceId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(serviceOfferingRepository.findById(serviceId)).isEmpty();
    }

    @Test
    @Transactional
    void delete_AppliesSoftDelete() {
        // GIVEN: An active service
        Provider provider = createProvider("provider", "provider@test.com");
        ServiceOffering service = createService(provider, "Test Service", BigDecimal.valueOf(100));
        testEntityManager.flush();
        final Long serviceId = service.getId();

        // WHEN: Deleting the service entity
        serviceOfferingRepository.delete(service);
        testEntityManager.flush();

        // THEN: Service should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_service WHERE id = :id")
            .setParameter("id", serviceId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(serviceOfferingRepository.findById(serviceId)).isEmpty();
    }

    @Test
    @Transactional
    void deleteAll_AppliesSoftDelete() {
        // GIVEN: 3 active services
        Provider provider = createProvider("provider", "provider@test.com");

        ServiceOffering service1 = createService(provider, "Service 1", BigDecimal.valueOf(100));
        ServiceOffering service2 = createService(provider, "Service 2", BigDecimal.valueOf(200));
        ServiceOffering service3 = createService(provider, "Service 3", BigDecimal.valueOf(300));
        testEntityManager.flush();

        List<ServiceOffering> services = List.of(service1, service2, service3);

        // WHEN: Deleting all services
        serviceOfferingRepository.deleteAll(services);
        testEntityManager.flush();

        // THEN: All should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Long deletedCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM t_service WHERE deleted_at IS NOT NULL AND id IN (:ids)")
            .setParameter("ids", services.stream().map(ServiceOffering::getId).toList())
            .getSingleResult();

        assertThat(deletedCount).isEqualTo(3);

        // And repository should return empty
        testEntityManager.clear();
        assertThat(serviceOfferingRepository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    void deleteAll_NoArguments_AppliesSoftDeleteToAll() {
        // GIVEN: 4 services
        Provider provider = createProvider("provider", "provider@test.com");

        for (int i = 1; i <= 4; i++) {
            createService(provider, "Service " + i, BigDecimal.valueOf(100 * i));
        }
        testEntityManager.flush();

        // WHEN: Deleting all services
        serviceOfferingRepository.deleteAll();
        testEntityManager.flush();

        // THEN: All should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Long deletedCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM t_service WHERE deleted_at IS NOT NULL")
            .getSingleResult();

        assertThat(deletedCount).isEqualTo(4);

        // And repository should return empty
        testEntityManager.clear();
        assertThat(serviceOfferingRepository.findAll()).isEmpty();
        assertThat(serviceOfferingRepository.count()).isEqualTo(0);
    }

    @Test
    @Transactional
    void verifyDeletedAtTimestamp_IsSetCorrectly() {
        // GIVEN: An active service
        Provider provider = createProvider("provider", "provider@test.com");
        ServiceOffering service = createService(provider, "Test Service", BigDecimal.valueOf(100));
        testEntityManager.flush();
        final Long serviceId = service.getId();

        LocalDateTime beforeDelete = LocalDateTime.now().minusSeconds(1); // Allow 1 second tolerance

        // WHEN: Soft deleting the service
        serviceOfferingRepository.deleteById(serviceId);
        testEntityManager.flush();

        LocalDateTime afterDelete = LocalDateTime.now().plusSeconds(1); // Allow 1 second tolerance

        // THEN: deleted_at should be set within the time window
        EntityManager em = testEntityManager.getEntityManager();
        java.sql.Timestamp deletedAtTimestamp = (java.sql.Timestamp) em.createNativeQuery(
                "SELECT deleted_at FROM t_service WHERE id = :id")
            .setParameter("id", serviceId)
            .getSingleResult();

        assertThat(deletedAtTimestamp).isNotNull();
        LocalDateTime deletedAt = deletedAtTimestamp.toLocalDateTime();
        assertThat(deletedAt).isAfterOrEqualTo(beforeDelete);
        assertThat(deletedAt).isBeforeOrEqualTo(afterDelete);
    }

    @Test
    @Transactional
    void mixedOperations_OnlyShowActiveServices() {
        // GIVEN: Mix of active and soft deleted services
        Provider provider = createProvider("provider", "provider@test.com");

        ServiceOffering activeService = createService(provider, "Active Service", BigDecimal.valueOf(100));
        ServiceOffering deletedService = createService(provider, "Deleted Service", BigDecimal.valueOf(200));
        testEntityManager.flush();

        // Soft delete one service
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_service SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", deletedService.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN/THEN: All operations should exclude soft deleted service
        assertThat(serviceOfferingRepository.findAll()).hasSize(1);
        assertThat(serviceOfferingRepository.count()).isEqualTo(1);
        assertThat(serviceOfferingRepository.findById(deletedService.getId())).isEmpty();
        assertThat(serviceOfferingRepository.findById(activeService.getId())).isPresent();
        assertThat(serviceOfferingRepository.existsById(deletedService.getId())).isFalse();
        assertThat(serviceOfferingRepository.existsById(activeService.getId())).isTrue();
    }

    @Test
    @Transactional
    void multipleProviders_SoftDeleteAffectsOnlyDeletedServices() {
        // GIVEN: Services from multiple providers, some soft deleted
        Provider provider1 = createProvider("provider1", "provider1@test.com");
        Provider provider2 = createProvider("provider2", "provider2@test.com");

        ServiceOffering service1_active = createService(provider1, "Provider1 Active", BigDecimal.valueOf(100));
        ServiceOffering service1_deleted = createService(provider1, "Provider1 Deleted", BigDecimal.valueOf(150));
        ServiceOffering service2_active = createService(provider2, "Provider2 Active", BigDecimal.valueOf(200));
        ServiceOffering service2_deleted = createService(provider2, "Provider2 Deleted", BigDecimal.valueOf(250));
        testEntityManager.flush();

        // Soft delete services from both providers
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_service SET deleted_at = NOW() WHERE id IN (:id1, :id2)")
            .setParameter("id1", service1_deleted.getId())
            .setParameter("id2", service2_deleted.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding all services
        List<ServiceOffering> result = serviceOfferingRepository.findAll();

        // THEN: Should return only active services from both providers
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(ServiceOffering::getName)
            .containsExactlyInAnyOrder("Provider1 Active", "Provider2 Active");
    }

    @Test
    @Transactional
    void saveAndFlush_DoesNotResurrectSoftDeletedService() {
        // GIVEN: A service that is soft deleted
        Provider provider = createProvider("provider", "provider@test.com");
        ServiceOffering service = createService(provider, "Test Service", BigDecimal.valueOf(100));
        testEntityManager.flush();
        final Long serviceId = service.getId();

        // Soft delete the service
        serviceOfferingRepository.deleteById(serviceId);
        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Trying to find and update the soft deleted service
        Optional<ServiceOffering> foundService = serviceOfferingRepository.findById(serviceId);

        // THEN: Service should not be found
        assertThat(foundService).isEmpty();

        // Verify it's still soft deleted in database
        EntityManager em = testEntityManager.getEntityManager();
        Object deletedAt = em.createNativeQuery(
                "SELECT deleted_at FROM t_service WHERE id = :id")
            .setParameter("id", serviceId)
            .getSingleResult();

        assertThat(deletedAt).isNotNull();
    }
}