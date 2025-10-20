package fi.unfinitas.bookora.dto.response;

import java.time.LocalDateTime;

/**
 * DTO for booking response.
 * Contains booking details with service and customer information.
 */
public record BookingResponse(
    Long id,
    ServiceResponse service,
    String customerName,
    String customerEmail,
    String customerPhone,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String status,
    String notes,
    LocalDateTime createdAt
) {}
