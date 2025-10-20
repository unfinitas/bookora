package fi.unfinitas.bookora.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for validating guest access token.
 * Used by guests to validate their token and auto-confirm pending bookings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTokenRequest {

    /**
     * The guest access token to validate
     */
    @NotNull(message = "Token is required")
    private UUID token;
}
