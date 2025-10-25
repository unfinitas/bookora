package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.GuestAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for GuestAccessToken entity.
 */
public interface GuestAccessTokenRepository extends JpaRepository<GuestAccessToken, Long> {

    /**
     * Find a guest access token by its UUID token value.
     *
     * @param token the UUID token
     * @return Optional containing the token if found
     */
    Optional<GuestAccessToken> findByToken(UUID token);

    /**
     * Find a guest access token by the booking ID.
     *
     * @param bookingId the booking ID
     * @return Optional containing the token if found
     */
    Optional<GuestAccessToken> findByBookingId(Long bookingId);
}
