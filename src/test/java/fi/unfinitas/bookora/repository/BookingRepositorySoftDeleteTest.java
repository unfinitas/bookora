package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.enums.BookingStatus;
import fi.unfinitas.bookora.domain.model.Booking;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RepositoryTestConfiguration.class)
@ActiveProfiles("test")
class BookingRepositorySoftDeleteTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private static class ProviderWithService {
        Provider provider;
        ServiceOffering service;

        ProviderWithService(Provider provider, ServiceOffering service) {
            this.provider = provider;
            this.service = service;
        }
    }

    private ProviderWithService setupProviderWithService() {
        User providerUser = TestDataBuilder.user()
            .username("provider")
            .email("provider@test.com")
            .build();
        testEntityManager.persist(providerUser);

        Provider provider = TestDataBuilder.provider()
            .user(providerUser)
            .build();
        testEntityManager.persist(provider);

        ServiceOffering service = TestDataBuilder.serviceOffering()
            .provider(provider)
            .build();
        testEntityManager.persist(service);

        return new ProviderWithService(provider, service);
    }

    private User setupCustomer(String username, String email) {
        User customer = TestDataBuilder.guestUser()
            .username(username)
            .email(email)
            .build();
        return testEntityManager.persist(customer);
    }

    @Test
    @Transactional
    void findById_SoftDeletedBooking_ReturnsEmpty() {
        // GIVEN: A booking that is soft deleted
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer1", "customer1@test.com");

        Booking booking = TestDataBuilder.booking()
            .provider(ps.provider)
            .customer(customer)
            .serviceOffering(ps.service)
            .build();
        booking = testEntityManager.persistAndFlush(booking);
        final Long bookingId = booking.getId();

        // Soft delete the booking
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW(), updated_at = NOW() WHERE id = :id")
            .setParameter("id", bookingId)
            .executeUpdate();

        testEntityManager.clear();

        // WHEN: Finding by ID
        Optional<Booking> result = bookingRepository.findById(bookingId);

        // THEN: Should return empty
        assertThat(result).isEmpty();
    }

    @Test
    @Transactional
    void findByCustomerId_ExcludesSoftDeletedBookings() {
        // GIVEN: Customer has 3 active bookings and 2 soft deleted bookings
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");
        final UUID customerId = customer.getId();

        // Create 3 active bookings
        for (int i = 0; i < 3; i++) {
            Booking activeBooking = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 1))
                .endTime(LocalDateTime.now().plusDays(i + 1).plusHours(1))
                .build();
            testEntityManager.persist(activeBooking);
        }

        // Create 2 bookings to soft delete
        for (int i = 0; i < 2; i++) {
            Booking toDelete = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 10))
                .endTime(LocalDateTime.now().plusDays(i + 10).plusHours(1))
                .build();
            toDelete = testEntityManager.persist(toDelete);
            testEntityManager.flush();

            // Soft delete immediately
            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding bookings by customer ID
        List<Booking> result = bookingRepository.findByCustomerId(customerId);

        // THEN: Should return only active bookings
        assertThat(result).hasSize(3);
    }

    @Test
    @Transactional
    void findByProviderId_ExcludesSoftDeletedBookings() {
        // GIVEN: Provider has 4 active bookings and 2 soft deleted bookings
        ProviderWithService ps = setupProviderWithService();
        final UUID providerId = ps.provider.getId();

        // Create multiple customers
        User customer1 = setupCustomer("customer1", "customer1@test.com");
        User customer2 = setupCustomer("customer2", "customer2@test.com");

        // Create 4 active bookings
        for (int i = 0; i < 4; i++) {
            User customer = (i % 2 == 0) ? customer1 : customer2;
            Booking activeBooking = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 1))
                .endTime(LocalDateTime.now().plusDays(i + 1).plusHours(1))
                .build();
            testEntityManager.persist(activeBooking);
        }

        // Create 2 bookings to soft delete
        for (int i = 0; i < 2; i++) {
            Booking toDelete = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer1)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 10))
                .endTime(LocalDateTime.now().plusDays(i + 10).plusHours(1))
                .build();
            toDelete = testEntityManager.persist(toDelete);
            testEntityManager.flush();

            // Soft delete immediately
            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding bookings by provider ID
        List<Booking> result = bookingRepository.findByProviderId(providerId);

        // THEN: Should return only active bookings
        assertThat(result).hasSize(4);
    }

    @Test
    @Transactional
    void existsOverlappingBooking_SoftDeletedBooking_ReturnsFalse() {
        // GIVEN: A soft deleted booking from 10:00-11:00
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime endTime = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        Booking booking = TestDataBuilder.booking()
            .provider(ps.provider)
            .customer(customer)
            .serviceOffering(ps.service)
            .startTime(startTime)
            .endTime(endTime)
            .status(BookingStatus.CONFIRMED)
            .build();
        booking = testEntityManager.persistAndFlush(booking);

        // Soft delete the booking
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", booking.getId())
            .executeUpdate();

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Checking for overlap at the same time
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
            ps.provider.getId(),
            startTime,
            endTime
        );

        // THEN: Should return false (soft deleted bookings don't count)
        assertThat(hasOverlap).isFalse();
    }

    @Test
    @Transactional
    void existsCustomerOverlappingBooking_SoftDeletedBooking_ReturnsFalse() {
        // GIVEN: A soft deleted customer booking from 14:00-15:00
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime endTime = LocalDateTime.now().plusDays(1).withHour(15).withMinute(0);

        Booking booking = TestDataBuilder.booking()
            .provider(ps.provider)
            .customer(customer)
            .serviceOffering(ps.service)
            .startTime(startTime)
            .endTime(endTime)
            .status(BookingStatus.PENDING)
            .build();
        booking = testEntityManager.persistAndFlush(booking);

        // Soft delete the booking
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", booking.getId())
            .executeUpdate();

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Checking for customer overlap at the same time
        boolean hasOverlap = bookingRepository.existsCustomerOverlappingBooking(
            customer.getId(),
            startTime,
            endTime
        );

        // THEN: Should return false (soft deleted bookings don't count)
        assertThat(hasOverlap).isFalse();
    }

    @Test
    @Transactional
    void findAll_ExcludesSoftDeletedBookings() {
        // GIVEN: 5 active bookings and 3 soft deleted bookings
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        // Create 5 active bookings
        for (int i = 0; i < 5; i++) {
            Booking activeBooking = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 1))
                .endTime(LocalDateTime.now().plusDays(i + 1).plusHours(1))
                .build();
            testEntityManager.persist(activeBooking);
        }

        // Create 3 soft deleted bookings
        for (int i = 0; i < 3; i++) {
            Booking toDelete = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 10))
                .endTime(LocalDateTime.now().plusDays(i + 10).plusHours(1))
                .build();
            toDelete = testEntityManager.persist(toDelete);
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding all bookings
        List<Booking> result = bookingRepository.findAll();

        // THEN: Should return only active bookings
        assertThat(result).hasSize(5);
    }

    @Test
    @Transactional
    void findAll_Pageable_ExcludesSoftDeletedBookings() {
        // GIVEN: 6 active bookings and 4 soft deleted bookings
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        for (int i = 0; i < 6; i++) {
            Booking activeBooking = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 1))
                .endTime(LocalDateTime.now().plusDays(i + 1).plusHours(1))
                .build();
            testEntityManager.persist(activeBooking);
        }

        for (int i = 0; i < 4; i++) {
            Booking toDelete = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 10))
                .endTime(LocalDateTime.now().plusDays(i + 10).plusHours(1))
                .build();
            toDelete = testEntityManager.persist(toDelete);
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Finding with pagination
        Page<Booking> result = bookingRepository.findAll(PageRequest.of(0, 10));

        // THEN: Should return only active bookings
        assertThat(result.getTotalElements()).isEqualTo(6);
        assertThat(result.getContent()).hasSize(6);
    }

    @Test
    @Transactional
    void count_ExcludesSoftDeletedBookings() {
        // GIVEN: 7 active bookings and 3 soft deleted bookings
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        for (int i = 0; i < 7; i++) {
            Booking activeBooking = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 1))
                .endTime(LocalDateTime.now().plusDays(i + 1).plusHours(1))
                .build();
            testEntityManager.persist(activeBooking);
        }

        for (int i = 0; i < 3; i++) {
            Booking toDelete = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 10))
                .endTime(LocalDateTime.now().plusDays(i + 10).plusHours(1))
                .build();
            toDelete = testEntityManager.persist(toDelete);
            testEntityManager.flush();

            EntityManager em = testEntityManager.getEntityManager();
            em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW() WHERE id = :id")
                .setParameter("id", toDelete.getId())
                .executeUpdate();
        }

        testEntityManager.flush();
        testEntityManager.clear();

        // WHEN: Counting bookings
        long count = bookingRepository.count();

        // THEN: Should count only active bookings
        assertThat(count).isEqualTo(7);
    }

    @Test
    @Transactional
    void deleteById_AppliesSoftDelete() {
        // GIVEN: An active booking
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        Booking booking = TestDataBuilder.booking()
            .provider(ps.provider)
            .customer(customer)
            .serviceOffering(ps.service)
            .build();
        booking = testEntityManager.persistAndFlush(booking);
        final Long bookingId = booking.getId();

        // WHEN: Deleting the booking
        bookingRepository.deleteById(bookingId);
        testEntityManager.flush();

        // THEN: Booking should be soft deleted (not physically deleted)
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_booking WHERE id = :id")
            .setParameter("id", bookingId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(bookingRepository.findById(bookingId)).isEmpty();
    }

    @Test
    @Transactional
    void delete_AppliesSoftDelete() {
        // GIVEN: An active booking
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        Booking booking = TestDataBuilder.booking()
            .provider(ps.provider)
            .customer(customer)
            .serviceOffering(ps.service)
            .build();
        booking = testEntityManager.persistAndFlush(booking);
        final Long bookingId = booking.getId();

        // WHEN: Deleting the booking entity
        bookingRepository.delete(booking);
        testEntityManager.flush();

        // THEN: Booking should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Object result = em.createNativeQuery(
                "SELECT deleted_at FROM t_booking WHERE id = :id")
            .setParameter("id", bookingId)
            .getSingleResult();

        assertThat(result).isNotNull();

        // And should not be found via repository
        testEntityManager.clear();
        assertThat(bookingRepository.findById(bookingId)).isEmpty();
    }

    @Test
    @Transactional
    void deleteAll_AppliesSoftDelete() {
        // GIVEN: 3 active bookings
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        List<Booking> bookings = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Booking booking = TestDataBuilder.booking()
                .provider(ps.provider)
                .customer(customer)
                .serviceOffering(ps.service)
                .startTime(LocalDateTime.now().plusDays(i + 1))
                .endTime(LocalDateTime.now().plusDays(i + 1).plusHours(1))
                .build();
            bookings.add(testEntityManager.persist(booking));
        }
        testEntityManager.flush();

        // WHEN: Deleting all bookings
        bookingRepository.deleteAll(bookings);
        testEntityManager.flush();

        // THEN: All should be soft deleted
        EntityManager em = testEntityManager.getEntityManager();
        Long deletedCount = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM t_booking WHERE deleted_at IS NOT NULL AND id IN (:ids)")
            .setParameter("ids", bookings.stream().map(Booking::getId).toList())
            .getSingleResult();

        assertThat(deletedCount).isEqualTo(3);

        // And repository should return empty
        testEntityManager.clear();
        assertThat(bookingRepository.findAll()).isEmpty();
    }

    @Test
    @Transactional
    void verifyDeletedAtTimestamp_IsSetCorrectly() {
        // GIVEN: An active booking
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        Booking booking = TestDataBuilder.booking()
            .provider(ps.provider)
            .customer(customer)
            .serviceOffering(ps.service)
            .build();
        booking = testEntityManager.persistAndFlush(booking);
        final Long bookingId = booking.getId();

        LocalDateTime beforeDelete = LocalDateTime.now().minusSeconds(1); // Allow 1 second tolerance

        // WHEN: Soft deleting the booking
        bookingRepository.deleteById(bookingId);
        testEntityManager.flush();

        LocalDateTime afterDelete = LocalDateTime.now().plusSeconds(1); // Allow 1 second tolerance

        // THEN: deleted_at should be set within the time window
        EntityManager em = testEntityManager.getEntityManager();
        java.sql.Timestamp deletedAtTimestamp = (java.sql.Timestamp) em.createNativeQuery(
                "SELECT deleted_at FROM t_booking WHERE id = :id")
            .setParameter("id", bookingId)
            .getSingleResult();

        assertThat(deletedAtTimestamp).isNotNull();
        LocalDateTime deletedAt = deletedAtTimestamp.toLocalDateTime();
        assertThat(deletedAt).isAfterOrEqualTo(beforeDelete);
        assertThat(deletedAt).isBeforeOrEqualTo(afterDelete);
    }

    @Test
    @Transactional
    void mixedOperations_OnlyShowActiveBookings() {
        // GIVEN: Mix of active and soft deleted bookings
        ProviderWithService ps = setupProviderWithService();
        User customer = setupCustomer("customer", "customer@test.com");

        // Create active booking
        Booking activeBooking = TestDataBuilder.booking()
            .provider(ps.provider)
            .customer(customer)
            .serviceOffering(ps.service)
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
            .status(BookingStatus.CONFIRMED)
            .build();
        activeBooking = testEntityManager.persist(activeBooking);

        // Create booking to be soft deleted
        Booking deletedBooking = TestDataBuilder.booking()
            .provider(ps.provider)
            .customer(customer)
            .serviceOffering(ps.service)
            .startTime(LocalDateTime.now().plusDays(2))
            .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
            .status(BookingStatus.CONFIRMED)
            .build();
        deletedBooking = testEntityManager.persist(deletedBooking);
        testEntityManager.flush();

        // Soft delete one booking
        EntityManager em = testEntityManager.getEntityManager();
        em.createNativeQuery("UPDATE t_booking SET deleted_at = NOW() WHERE id = :id")
            .setParameter("id", deletedBooking.getId())
            .executeUpdate();

        testEntityManager.clear();

        // WHEN/THEN: All operations should exclude soft deleted booking
        assertThat(bookingRepository.findAll()).hasSize(1);
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(bookingRepository.findByCustomerId(customer.getId())).hasSize(1);
        assertThat(bookingRepository.findByProviderId(ps.provider.getId())).hasSize(1);

        // Overlap checks should not consider soft-deleted bookings
        assertThat(bookingRepository.existsOverlappingBooking(
            ps.provider.getId(),
            deletedBooking.getStartTime(),
            deletedBooking.getEndTime()
        )).isFalse();

        assertThat(bookingRepository.existsCustomerOverlappingBooking(
            customer.getId(),
            deletedBooking.getStartTime(),
            deletedBooking.getEndTime()
        )).isFalse();
    }
}