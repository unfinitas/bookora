package fi.unfinitas.bookora.dto.response;

import java.util.UUID;

/**
 * DTO for login response.
 */
public record LoginResponse(
    UUID id,
    String username,
    String role,
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn // Access token expiration in milliseconds
) {}
