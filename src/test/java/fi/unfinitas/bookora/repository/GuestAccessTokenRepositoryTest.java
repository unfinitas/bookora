package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.model.*;
import fi.unfinitas.bookora.testutil.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RepositoryTestConfiguration.class)
@ActiveProfiles("test")
class GuestAccessTokenRepositoryTest {

    @Autowired
    private GuestAccessTokenRepository tokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByToken_ExistingToken_ReturnsToken() {
        // GIVEN: Token exists in database
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final Booking booking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .build();
        entityManager.persist(booking);

        final GuestAccessToken token = TestDataBuilder.guestAccessToken()
            .booking(booking)
            .build();
        final GuestAccessToken saved = entityManager.persistAndFlush(token);

        // WHEN: findByToken is called
        final Optional<GuestAccessToken> result = tokenRepository.findByToken(saved.getToken());

        // THEN: Returns Optional<GuestAccessToken>
        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo(saved.getToken());
    }

    @Test
    void findByToken_NonExistentToken_ReturnsEmpty() {
        // GIVEN: Token does not exist
        final UUID nonExistentToken = UUID.randomUUID();

        // WHEN: findByToken is called
        final Optional<GuestAccessToken> result = tokenRepository.findByToken(nonExistentToken);

        // THEN: Returns Optional.empty()
        assertThat(result).isEmpty();
    }

    @Test
    void findByBookingId_ExistingBooking_ReturnsToken() {
        // GIVEN: Booking has associated token
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final Booking booking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .build();
        final Booking savedBooking = entityManager.persist(booking);

        final GuestAccessToken token = TestDataBuilder.guestAccessToken()
            .booking(savedBooking)
            .build();
        entityManager.persistAndFlush(token);

        // WHEN: findByBookingId is called
        final Optional<GuestAccessToken> result = tokenRepository.findByBookingId(savedBooking.getId());

        // THEN: Returns Optional<GuestAccessToken>
        assertThat(result).isPresent();
        assertThat(result.get().getBooking().getId()).isEqualTo(savedBooking.getId());
    }

    @Test
    void findByBookingId_NoToken_ReturnsEmpty() {
        // GIVEN: Booking has no token
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final Booking booking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .build();
        final Booking savedBooking = entityManager.persistAndFlush(booking);

        // WHEN: findByBookingId is called
        final Optional<GuestAccessToken> result = tokenRepository.findByBookingId(savedBooking.getId());

        // THEN: Returns Optional.empty()
        assertThat(result).isEmpty();
    }

    @Test
    void save_NewToken_SavesSuccessfully() {
        // GIVEN: New GuestAccessToken
        final User providerUser = TestDataBuilder.user().build();
        entityManager.persist(providerUser);
        final Provider provider = TestDataBuilder.provider().user(providerUser).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        entityManager.persist(serviceOffering);

        final User customer = TestDataBuilder.guestUser().build();
        entityManager.persist(customer);

        final Booking booking = TestDataBuilder.booking()
            .customer(customer)
            .provider(provider)
            .serviceOffering(serviceOffering)
            .build();
        entityManager.persist(booking);

        final GuestAccessToken token = TestDataBuilder.guestAccessToken()
            .booking(booking)
            .build();

        // WHEN: save is called
        final GuestAccessToken saved = tokenRepository.save(token);
        entityManager.flush();

        // THEN: Token is persisted with ID
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getToken()).isNotNull();
    }
}
