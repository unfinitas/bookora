package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.enums.BookingStatus;
import fi.unfinitas.bookora.domain.model.Booking;
import fi.unfinitas.bookora.domain.model.Provider;
import fi.unfinitas.bookora.domain.model.ServiceOffering;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.testutil.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RepositoryTestConfiguration.class)
@ActiveProfiles("test")
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByCustomerId_ExistingCustomer_ReturnsBookings() {
        // GIVEN: Customer has 3 bookings
        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final User providerUser = TestDataBuilder.user().username("provider").email("provider@test.com").build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final Booking booking1 = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .build();
        final Booking booking2 = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(LocalDateTime.now().plusDays(3))
            .endTime(LocalDateTime.now().plusDays(3).plusHours(1))
            .build();
        final Booking booking3 = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(LocalDateTime.now().plusDays(4))
            .endTime(LocalDateTime.now().plusDays(4).plusHours(1))
            .build();

        entityManager.persist(booking1);
        entityManager.persist(booking2);
        entityManager.persist(booking3);
        entityManager.flush();

        // WHEN: findByCustomerId is called
        final List<Booking> result = bookingRepository.findByCustomerId(customer.getId());

        // THEN: Returns list with 3 bookings
        assertThat(result).hasSize(3);
    }

    @Test
    void findByCustomerId_NoBookings_ReturnsEmptyList() {
        // GIVEN: Customer has no bookings
        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persistAndFlush(customer);

        // WHEN: findByCustomerId is called
        final List<Booking> result = bookingRepository.findByCustomerId(customer.getId());

        // THEN: Returns empty list
        assertThat(result).isEmpty();
    }

    @Test
    void findByProviderId_ExistingProvider_ReturnsBookings() {
        // GIVEN: Provider has 2 bookings
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final Booking booking1 = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .build();
        final Booking booking2 = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(LocalDateTime.now().plusDays(3))
            .endTime(LocalDateTime.now().plusDays(3).plusHours(1))
            .build();

        entityManager.persist(booking1);
        entityManager.persist(booking2);
        entityManager.flush();

        // WHEN: findByProviderId is called
        final List<Booking> result = bookingRepository.findByProviderId(provider.getId());

        // THEN: Returns list with 2 bookings
        assertThat(result).hasSize(2);
    }

    @Test
    void existsOverlappingBooking_OverlapExists_ReturnsTrue() {
        // GIVEN: Booking exists from 10:00-11:00 with status PENDING
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .status(BookingStatus.PENDING)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for overlap 10:30-11:30
        final LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(30);
        final LocalDateTime newEnd = LocalDateTime.now().plusDays(1).withHour(11).withMinute(30);

        final boolean result = bookingRepository.existsOverlappingBooking(
            provider.getId(),
            newStart,
            newEnd
        );

        // THEN: Returns true
        assertThat(result).isTrue();
    }

    @Test
    void existsOverlappingBooking_NoOverlap_ReturnsFalse() {
        // GIVEN: Booking exists from 10:00-11:00
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for overlap 11:00-12:00 (no overlap)
        final LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);
        final LocalDateTime newEnd = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);

        final boolean result = bookingRepository.existsOverlappingBooking(
            provider.getId(),
            newStart,
            newEnd
        );

        // THEN: Returns false
        assertThat(result).isFalse();
    }

    @Test
    void existsOverlappingBooking_CancelledBooking_ReturnsFalse() {
        // GIVEN: Booking exists from 10:00-11:00 with status CANCELLED
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .status(BookingStatus.CANCELLED)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for overlap 10:00-11:00
        final boolean result = bookingRepository.existsOverlappingBooking(
            provider.getId(),
            start,
            end
        );

        // THEN: Returns false (cancelled bookings don't count)
        assertThat(result).isFalse();
    }

    @Test
    void existsOverlappingBooking_PartialOverlap_ReturnsTrue() {
        // GIVEN: Booking exists from 10:00-11:00
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for overlap 09:30-10:30
        final LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(9).withMinute(30);
        final LocalDateTime newEnd = LocalDateTime.now().plusDays(1).withHour(10).withMinute(30);

        final boolean result = bookingRepository.existsOverlappingBooking(
            provider.getId(),
            newStart,
            newEnd
        );

        // THEN: Returns true
        assertThat(result).isTrue();
    }

    @Test
    void existsOverlappingBooking_ExactSameTime_ReturnsTrue() {
        // GIVEN: Booking exists from 10:00-11:00
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for overlap 10:00-11:00
        final boolean result = bookingRepository.existsOverlappingBooking(
            provider.getId(),
            start,
            end
        );

        // THEN: Returns true
        assertThat(result).isTrue();
    }

    @Test
    void existsCustomerOverlappingBooking_OverlapExists_ReturnsTrue() {
        // GIVEN: Customer has booking from 10:00-11:00 with status CONFIRMED
        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .status(BookingStatus.CONFIRMED)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for customer overlap 10:30-11:30
        final LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(30);
        final LocalDateTime newEnd = LocalDateTime.now().plusDays(1).withHour(11).withMinute(30);

        final boolean result = bookingRepository.existsCustomerOverlappingBooking(
            customer.getId(),
            newStart,
            newEnd
        );

        // THEN: Returns true
        assertThat(result).isTrue();
    }

    @Test
    void existsCustomerOverlappingBooking_NoOverlap_ReturnsFalse() {
        // GIVEN: Customer has booking from 10:00-11:00
        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for customer overlap 11:00-12:00 (no overlap)
        final LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);
        final LocalDateTime newEnd = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0);

        final boolean result = bookingRepository.existsCustomerOverlappingBooking(
            customer.getId(),
            newStart,
            newEnd
        );

        // THEN: Returns false
        assertThat(result).isFalse();
    }

    @Test
    void existsCustomerOverlappingBooking_CancelledBooking_ReturnsFalse() {
        // GIVEN: Customer has booking from 10:00-11:00 with status CANCELLED
        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .status(BookingStatus.CANCELLED)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for customer overlap 10:00-11:00
        final boolean result = bookingRepository.existsCustomerOverlappingBooking(
            customer.getId(),
            start,
            end
        );

        // THEN: Returns false (cancelled bookings don't count)
        assertThat(result).isFalse();
    }

    @Test
    void existsCustomerOverlappingBooking_PartialOverlap_ReturnsTrue() {
        // GIVEN: Customer has booking from 10:00-11:00
        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for customer overlap 09:30-10:30
        final LocalDateTime newStart = LocalDateTime.now().plusDays(1).withHour(9).withMinute(30);
        final LocalDateTime newEnd = LocalDateTime.now().plusDays(1).withHour(10).withMinute(30);

        final boolean result = bookingRepository.existsCustomerOverlappingBooking(
            customer.getId(),
            newStart,
            newEnd
        );

        // THEN: Returns true
        assertThat(result).isTrue();
    }

    @Test
    void existsCustomerOverlappingBooking_ExactSameTime_ReturnsTrue() {
        // GIVEN: Customer has booking from 10:00-11:00
        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for customer overlap 10:00-11:00
        final boolean result = bookingRepository.existsCustomerOverlappingBooking(
            customer.getId(),
            start,
            end
        );

        // THEN: Returns true
        assertThat(result).isTrue();
    }

    @Test
    void existsCustomerOverlappingBooking_DifferentCustomer_ReturnsFalse() {
        // GIVEN: Customer A has booking from 10:00-11:00
        final User customerA = TestDataBuilder.guestUser().username("guestuser-a").email("customer-a@test.com").build();
        entityManager.persist(customerA);

        final User customerB = TestDataBuilder.guestUser().username("guestuser-b").email("customer-b@test.com").build();
        entityManager.persist(customerB);

        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final LocalDateTime end = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0);

        final Booking existingBooking = TestDataBuilder.booking()
            .customer(customerA)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .startTime(start)
            .endTime(end)
            .build();
        entityManager.persistAndFlush(existingBooking);

        // WHEN: Check for customer B overlap 10:00-11:00
        final boolean result = bookingRepository.existsCustomerOverlappingBooking(
            customerB.getId(),
            start,
            end
        );

        // THEN: Returns false (different customer)
        assertThat(result).isFalse();
    }
}
