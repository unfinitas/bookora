package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
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
class UserRepositorySoftDeleteTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    @Transactional
    void findById_SoftDeletedUser_ReturnsEmpty() {
        // GIVEN: A user that is soft deleted
        User user = TestDataBuilder.user().build();
        user = testEntityManager.persistAndFlush(user);
        final UUID userId = user.getId();

        // Soft delete the user using native query
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_user SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", userId)
            .executeUpdate();

        // Clear persistence context to bypass cache
        testEntityManager.clear();

        // WHEN: Finding by ID
        Optional<User> result = userRepository.findById(userId);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findByEmail_SoftDeletedUser_ReturnsEmpty() {
        // GIVEN: A user that is soft deleted
        final String email = "softdeleted@test.com";
        User user = TestDataBuilder.user()
            .email(email)
            .build();
        user = testEntityManager.persistAndFlush(user);

        // Soft delete the user
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_user SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", user.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding by email
        Optional<User> result = userRepository.findByEmail(email);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findByUsername_SoftDeletedUser_ReturnsEmpty() {
        // GIVEN: A user that is soft deleted
        final String username = "softdeleteduser";
        User user = TestDataBuilder.user()
            .username(username)
            .build();
        user = testEntityManager.persistAndFlush(user);

        // Soft delete the user
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_user SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", user.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding by username
        Optional<User> result = userRepository.findByUsername(username);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void existsByEmail_SoftDeletedUser_ReturnsFalse() {
        // GIVEN: A user that is soft deleted
        final String email = "softdeleted@test.com";
        User user = TestDataBuilder.user()
            .email(email)
            .build();
        user = testEntityManager.persistAndFlush(user);

        // Soft delete the user
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_user SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", user.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Checking existence by email
        boolean exists = userRepository.existsByEmail(email);

        // THEN: Should return false
        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    void existsByUsername_SoftDeletedUser_ReturnsFalse() {
        // GIVEN: A user that is soft deleted
        final String username = "softdeleteduser";
        User user = TestDataBuilder.user()
            .username(username)
            .build();
        user = testEntityManager.persistAndFlush(user);

        // Soft delete the user
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_user SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", user.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Checking existence by username
        boolean exists = userRepository.existsByUsername(username);

        // THEN: Should return false
        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    void findAll_ExcludesSoftDeletedUsers() {
        // GIVEN: 3 active users and 2 soft deleted users
        User activeUser1 = TestDataBuilder.user()
            .username("active1")
            .email("active1@test.com")
            .build();
        User activeUser2 = TestDataBuilder.user()
            .username("active2")
            .email("active2@test.com")
            .build();
        User activeUser3 = TestDataBuilder.user()
            .username("active3")
            .email("active3@test.com")
            .build();
        User deletedUser1 = TestDataBuilder.user()
            .username("deleted1")
            .email("deleted1@test.com")
            .build();
        User deletedUser2 = TestDataBuilder.user()
            .username("deleted2")
            .email("deleted2@test.com")
            .build();

        testEntityManager.persist(activeUser1);
        testEntityManager.persist(activeUser2);
        testEntityManager.persist(activeUser3);
        deletedUser1 = testEntityManager.persist(deletedUser1);
        deletedUser2 = testEntityManager.persist(deletedUser2);
        testEntityManager.flush();

        // Soft delete two users
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_user SET deleted_at = NOW(), updated_at = NOW() WHERE id IN (:id1, :id2)")
            .setParameter("id1", deletedUser1.getId())
            .setParameter("id2", deletedUser2.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding all users
        List<User> result = userRepository.findAll();

        // THEN: Should return only active users
        assertThat(result).hasSize(3);
        assertThat(result)
            .extracting(User::getUsername)
            .containsExactlyInAnyOrder("active1", "active2", "active3");
    }

    @Test
    @Transactional
    void findAll_Pageable_ExcludesSoftDeletedUsers() {
        // GIVEN: 5 active users and 3 soft deleted users
        for (int i = 1; i <= 5; i++) {
            User activeUser = TestDataBuilder.user()
                .username("active" + i)
                .email("active" + i + "@test.com")
                .build();
            testEntityManager.persist(activeUser);
        }

        for (int i = 1; i <= 3; i++) {
            User deletedUser = TestDataBuilder.user()
                .username("deleted" + i)
                .email("deleted" + i + "@test.com")
                .build();
            deletedUser = testEntityManager.persist(deletedUser);

            // Soft delete immediately
            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_user SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
                .setParameter("id", deletedUser.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding all with pagination
        Page<User> result = userRepository.findAll(PageRequest.of(0, 10));

        // THEN: Should return only active users
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getContent())
            .extracting(User::getUsername)
            .allMatch(username -> username.startsWith("active"));
    }

    @Test
    @Transactional
    void count_ExcludesSoftDeletedUsers() {
        // GIVEN: 4 active users and 2 soft deleted users
        for (int i = 1; i <= 4; i++) {
            User activeUser = TestDataBuilder.user()
                .username("active" + i)
                .email("active" + i + "@test.com")
                .build();
            testEntityManager.persist(activeUser);
        }

        for (int i = 1; i <= 2; i++) {
            User deletedUser = TestDataBuilder.user()
                .username("deleted" + i)
                .email("deleted" + i + "@test.com")
                .build();
            deletedUser = testEntityManager.persist(deletedUser);

            // Soft delete immediately
            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_user SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
                .setParameter("id", deletedUser.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Counting users
        long count = userRepository.count();

        // THEN: Should count only active users
        assertThat(count).isEqualTo(4);
    }

    @Test
    @Transactional
    void deleteById_AppliesSoftDelete() {
        // GIVEN: An active user
        User user = TestDataBuilder.user().build();
        user = testEntityManager.persistAndFlush(user);
        final UUID userId = user.getId();

        // WHEN: Deleting the user
        userRepository.deleteById(userId);
        testEntityManager.flush();

        // THEN: User should be soft deleted (not physically deleted)
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_user WHERE id = :id")
            .setParameter("id", userId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    @Transactional
    void delete_AppliesSoftDelete() {
        // GIVEN: An active user
        User user = TestDataBuilder.user().build();
        user = testEntityManager.persistAndFlush(user);
        final UUID userId = user.getId();

        // WHEN: Deleting the user entity
        userRepository.delete(user);
        testEntityManager.flush();

        // THEN: User should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_user WHERE id = :id")
            .setParameter("id", userId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    @Transactional
    void deleteAll_AppliesSoftDeleteToAllUsers() {
        // GIVEN: 3 active users
        User user1 = TestDataBuilder.user()
            .username("user1")
            .email("user1@test.com")
            .build();
        User user2 = TestDataBuilder.user()
            .username("user2")
            .email("user2@test.com")
            .build();
        User user3 = TestDataBuilder.user()
            .username("user3")
            .email("user3@test.com")
            .build();

        user1 = testEntityManager.persist(user1);
        user2 = testEntityManager.persist(user2);
        user3 = testEntityManager.persist(user3);
        testEntityManager.flush();

        List<User> users = List.of(user1, user2, user3);

        // WHEN: Deleting all users
        userRepository.deleteAll(users);
        testEntityManager.flush();

        // THEN: All users should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Long deletedCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM t_user WHERE deleted_at IS NOT NULL AND id IN (:ids)")
            .setParameter("ids", users.stream().map(User::getId).toList())
            .getSingleResult();

        assertThat(deletedCount).isEqualTo(3);

        // And repository should return empty
        testEntityManager.clear();
        assertThat(userRepository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    void mixedOperations_OnlyShowActiveUsers() {
        // GIVEN: Mix of active and soft deleted users
        User activeUser = TestDataBuilder.user()
            .username("activeuser")
            .email("active@test.com")
            .build();
        User deletedUser = TestDataBuilder.user()
            .username("deleteduser")
            .email("deleted@test.com")
            .build();

        activeUser = testEntityManager.persist(activeUser);
        deletedUser = testEntityManager.persist(deletedUser);
        testEntityManager.flush();

        // Soft delete one user
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_user SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", deletedUser.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN/THEN: All operations should exclude soft deleted user
        assertThat(userRepository.findAll()).hasSize(1);
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmail("deleted@test.com")).isEmpty();
        assertThat(userRepository.findByEmail("active@test.com")).isPresent();
        assertThat(userRepository.existsByEmail("deleted@test.com")).isFalse();
        assertThat(userRepository.existsByEmail("active@test.com")).isTrue();
    }

    @Test
    @Transactional
    void verifyDeletedAtTimestamp_IsSetCorrectly() {
        // GIVEN: An active user
        User user = TestDataBuilder.user().build();
        user = testEntityManager.persistAndFlush(user);
        final UUID userId = user.getId();

        LocalDateTime beforeDelete = LocalDateTime.now().minusSeconds(1); // Allow 1 second tolerance

        // WHEN: Soft deleting the user
        userRepository.deleteById(userId);
        testEntityManager.flush();

        LocalDateTime afterDelete = LocalDateTime.now().plusSeconds(1); // Allow 1 second tolerance

        // THEN: deleted_at should be set within the time window
        EntityManager em = testEntityManager.getEntityManager();
        java.sql.Timestamp deletedAtTimestamp = (java.sql.Timestamp) em.createNativeQuery(
                "SELECT deleted_at FROM t_user WHERE id = :id")
            .setParameter("id", userId)
            .getSingleResult();

        assertThat(deletedAtTimestamp).isNotNull();
        LocalDateTime deletedAt = deletedAtTimestamp.toLocalDateTime();
        assertThat(deletedAt).isAfterOrEqualTo(beforeDelete);
        assertThat(deletedAt).isBeforeOrEqualTo(afterDelete);
    }
}