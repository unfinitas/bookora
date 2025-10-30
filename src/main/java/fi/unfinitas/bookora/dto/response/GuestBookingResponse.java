package fi.unfinitas.bookora.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for guest booking response.
 * Extends BookingResponse with access token information for guest users.
 */
public record GuestBookingResponse(
    Long id,
    ServiceOfferingResponse serviceOffering,
    String customerName,
    String customerEmail,
    String customerPhone,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status,
    String notes,
    LocalDateTime createdAt,
    UUID accessToken,
    LocalDateTime tokenExpiresAt
) {}
