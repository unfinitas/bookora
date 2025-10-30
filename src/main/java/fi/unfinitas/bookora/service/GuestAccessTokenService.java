package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.enums.BookingStatus;
import fi.unfinitas.bookora.domain.model.Booking;
import fi.unfinitas.bookora.domain.model.GuestAccessToken;
import fi.unfinitas.bookora.exception.BookingAlreadyCancelledException;
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
    private final BookoraProperties bookoraProperties;

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

        final int extensionDays = bookoraProperties.getGuest().getToken().getExpirationExtensionDays();
        final LocalDateTime expiresAt = booking.getEndTime().plusDays(extensionDays);

        final GuestAccessToken token = GuestAccessToken.builder()
                .booking(booking)
                .token(UUID.randomUUID())
                .expiresAt(expiresAt)
                .confirmedAt(null)
                .build();

        final GuestAccessToken savedToken = tokenRepository.save(token);
        log.debug("Token generated successfully. Expires at: {}", savedToken.getExpiresAt());

        return savedToken;
    }

    /**
     * Validate token and return the GuestAccessToken entity.
     * Checks if token exists, is not expired, and booking is not cancelled.
     * Used for operations that allow already-confirmed tokens (e.g., view, cancel).
     *
     * @param token the UUID token
     * @return the GuestAccessToken entity
     * @throws InvalidTokenException if token not found or booking is cancelled
     * @throws TokenExpiredException if token has expired
     */
    @Transactional(readOnly = true)
    public GuestAccessToken validateToken(final UUID token) {
        log.debug("Validating access token");

        final GuestAccessToken accessToken = findToken(token);
        validateExpiration(accessToken);
        validateBookingNotCancelled(accessToken);

        log.debug("Token validated successfully for booking ID: {}", accessToken.getBooking().getId());
        return accessToken;
    }

    /**
     * Validate token for confirmation operation.
     * Checks if token exists, is not expired, and has not been confirmed yet.
     * Used for confirmation operation to prevent double-confirmation.
     *
     * @param token the UUID token
     * @return the GuestAccessToken entity
     * @throws InvalidTokenException if token not found or already confirmed
     * @throws TokenExpiredException if token has expired
     */
    @Transactional(readOnly = true)
    public GuestAccessToken validateTokenForConfirm(final UUID token) {
        log.debug("Validating access token for confirmation");

        final GuestAccessToken accessToken = findToken(token);
        validateExpiration(accessToken);
        validateNotConfirmed(accessToken);

        log.debug("Token validated successfully for confirmation. Booking ID: {}",
                accessToken.getBooking().getId());
        return accessToken;
    }

    /**
     * Find token by UUID.
     *
     * @param token the UUID token
     * @return the GuestAccessToken entity
     * @throws InvalidTokenException if token not found
     */
    private GuestAccessToken findToken(final UUID token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Access token not found");
                    return new InvalidTokenException("Token not found");
                });
    }

    /**
     * Validate token expiration.
     *
     * @param accessToken the token to validate
     * @throws TokenExpiredException if token has expired
     */
    private void validateExpiration(final GuestAccessToken accessToken) {
        if (accessToken.isExpired()) {
            log.warn("Access token expired for booking ID: {}. Expired at: {}",
                    accessToken.getBooking().getId(), accessToken.getExpiresAt());
            throw new TokenExpiredException("Token expired");
        }
    }

    /**
     * Validate that token has not been confirmed yet.
     *
     * @param accessToken the token to validate
     * @throws InvalidTokenException if token was already confirmed
     */
    private void validateNotConfirmed(final GuestAccessToken accessToken) {
        if (accessToken.getConfirmedAt() != null) {
            log.warn("Attempt to reuse confirmed token for booking ID: {}",
                    accessToken.getBooking().getId());
            throw new InvalidTokenException("Token has already been used");
        }
    }

    /**
     * Validate that the booking associated with the token is not cancelled.
     * This prevents access to cancelled bookings through their tokens.
     *
     * @param accessToken the token to validate
     * @throws BookingAlreadyCancelledException if booking is cancelled
     */
    private void validateBookingNotCancelled(final GuestAccessToken accessToken) {
        if (accessToken.getBooking().getStatus() == BookingStatus.CANCELLED) {
            log.warn("Attempt to access cancelled booking ID: {}",
                    accessToken.getBooking().getId());
            throw new BookingAlreadyCancelledException(
                    "This booking has been cancelled and cannot be accessed"
            );
        }
    }
}
