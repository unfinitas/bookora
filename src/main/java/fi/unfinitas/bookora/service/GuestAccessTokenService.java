package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.model.Booking;
import fi.unfinitas.bookora.domain.model.GuestAccessToken;
import fi.unfinitas.bookora.exception.InvalidTokenException;
import fi.unfinitas.bookora.exception.TokenExpiredException;
import fi.unfinitas.bookora.repository.GuestAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing guest access tokens.
 * Handles token generation, validation, and lifecycle management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuestAccessTokenService {

    private final GuestAccessTokenRepository tokenRepository;

    /**
     * Generate a new access token for a booking.
     * Token expires at the booking's end time.
     *
     * @param booking the booking to create token for
     * @return the created guest access token
     */
    @Transactional
    public GuestAccessToken generateToken(final Booking booking) {
        log.debug("Generating access token for booking ID: {}", booking.getId());

        final GuestAccessToken token = GuestAccessToken.builder()
                .booking(booking)
                .token(UUID.randomUUID())
                .expiresAt(booking.getEndTime())
                .confirmedAt(null)
                .build();

        final GuestAccessToken savedToken = tokenRepository.save(token);
        log.debug("Token generated successfully. Expires at: {}", savedToken.getExpiresAt());

        return savedToken;
    }

    /**
     * Validate token and return the GuestAccessToken entity.
     * Checks if token exists and is not expired.
     * Does NOT mark as confirmed - that is handled by the business layer.
     *
     * @param token the UUID token
     * @return the GuestAccessToken entity
     * @throws InvalidTokenException if token not found
     * @throws TokenExpiredException if token has expired
     */
    @Transactional(readOnly = true)
    public GuestAccessToken validateToken(final UUID token) {
        log.debug("Validating access token");

        final GuestAccessToken accessToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Access token not found");
                    return new InvalidTokenException("Invalid or non-existent token");
                });

        if (accessToken.isExpired()) {
            log.warn("Access token expired for booking ID: {}. Expired at: {}",
                    accessToken.getBooking().getId(), accessToken.getExpiresAt());
            throw new TokenExpiredException("Access token has expired. Please create a new booking.");
        }

        log.debug("Token validated successfully for booking ID: {}", accessToken.getBooking().getId());
        return accessToken;
    }
}
