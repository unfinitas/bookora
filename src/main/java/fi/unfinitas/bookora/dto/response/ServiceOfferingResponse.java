package fi.unfinitas.bookora.dto.response;

import java.math.BigDecimal;

/**
 * DTO for service offering response.
 * Contains service offering details including provider information.
 */
public record ServiceOfferingResponse(
    Long id,
    String name,
    String description,
    Integer durationMinutes,
    BigDecimal price,
    String providerBusinessName
) {}
