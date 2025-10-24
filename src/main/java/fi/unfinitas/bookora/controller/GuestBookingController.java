package fi.unfinitas.bookora.controller;

import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;
import fi.unfinitas.bookora.dto.response.ApiResponse;
import fi.unfinitas.bookora.dto.response.BookingResponse;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;
import fi.unfinitas.bookora.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for guest booking operations.
 * All endpoints are public (no authentication required).
 * Guests can create, view, and cancel bookings using access tokens.
 */
@RestController
@RequestMapping("/bookings/guest")
@RequiredArgsConstructor
@Slf4j
public class GuestBookingController {

    private final BookingService bookingService;

    /**
     * Create a new guest booking.
     * Guest provides contact info and serviceId.
     * System generates access token for managing the booking.
     *
     * @param request the booking request with guest contact info and service details
     * @return guest booking response with access token
     */
    @PostMapping
    public ResponseEntity<ApiResponse<GuestBookingResponse>> createGuestBooking(
            @Valid @RequestBody final CreateGuestBookingRequest request) {
        final GuestBookingResponse response = bookingService.createGuestBooking(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Booking created successfully",
                        response
                ));
    }

    /**
     * Get booking details by access token.
     * Guests can view their booking using the token provided at creation.
     *
     * @param token the UUID access token
     * @return booking details
     */
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByToken(
            @PathVariable final UUID token) {
        final BookingResponse response = bookingService.getBookingByToken(token);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking retrieved successfully",
                        response
                )
        );
    }

    /**
     * Confirm booking by access token.
     * Changes status from PENDING to CONFIRMED and sets confirmed_at timestamp.
     * Can only be called once per booking (before booking start time).
     *
     * @param token the UUID access token
     * @return updated booking with CONFIRMED status
     */
    @PostMapping("/{token}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBookingByToken(
            @PathVariable final UUID token) {
        final BookingResponse response = bookingService.confirmBookingByToken(token);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking confirmed successfully",
                        response
                )
        );
    }

    /**
     * Cancel booking by access token.
     * Sets booking status to CANCELLED.
     * Can only be cancelled up to 24 hours before booking start time.
     * Token can still be used to view the cancelled booking.
     *
     * @param token the UUID access token
     * @return updated booking with CANCELLED status
     */
    @DeleteMapping("/{token}")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBookingByToken(
            @PathVariable final UUID token) {
        final BookingResponse response = bookingService.cancelBookingByToken(token);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking cancelled successfully",
                        response
                )
        );
    }
}
