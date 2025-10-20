package fi.unfinitas.bookora.dto.response;

import java.math.BigDecimal;

/**
 * DTO for service response.
 * Contains service details including provider information.
 */
public record ServiceResponse(
    Long id,
    String name,
    String description,
    Integer durationMinutes,
    BigDecimal price,
    String providerBusinessName
) {}
