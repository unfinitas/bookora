package fi.unfinitas.bookora.dto.response;

import java.time.LocalDateTime;

/**
 * DTO for booking response.
 * Contains booking details with service offering and customer information.
 */
public record BookingResponse(
    Long id,
    ServiceOfferingResponse serviceOffering,
    String customerName,
    String customerEmail,
    String customerPhone,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status,
    String notes,
    LocalDateTime createdAt
) {}
