package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;
import fi.unfinitas.bookora.dto.response.BookingResponse;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;

import java.util.UUID;

/**
 * Service interface for managing booking operations.
 * Handles guest booking creation, retrieval, and cancellation.
 */
public interface BookingService {

    /**
     * Create a new guest booking.
     * Validates times, checks for overlapping bookings, creates/reuses guest user,
     * and generates access token.
     *
     * @param request the booking request
     * @return guest booking response with access token
     */
    GuestBookingResponse createGuestBooking(CreateGuestBookingRequest request);

    /**
     * Get booking by access token.
     * Can be called multiple times in any booking state.
     *
     * @param token the UUID access token
     * @return booking response
     */
    BookingResponse getBookingByToken(UUID token);

    /**
     * Confirm booking by access token.
     * Changes status from PENDING to CONFIRMED and sets confirmed_at timestamp.
     * Can only be called once per booking (before booking start time).
     *
     * @param token the UUID access token
     * @return updated booking response
     */
    BookingResponse confirmBookingByToken(UUID token);

    /**
     * Cancel booking by access token.
     * Changes status to CANCELLED.
     * Can only be cancelled before the configured cancellation window (default: 24 hours before booking start time).
     * Cannot be undone after cancellation.
     *
     * @param token the UUID access token
     * @return updated booking response
     */
    BookingResponse cancelBookingByToken(UUID token);

}
