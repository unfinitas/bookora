package fi.unfinitas.bookora.dto.response;

import java.util.UUID;

/**
 * DTO for login response.
 * Refresh token is sent via HttpOnly cookie, not in response body.
 */
public record LoginResponse(
    UUID id,
    String username,
    String role,
    String accessToken,
    String tokenType,
    Long expiresIn // Access token expiration in milliseconds
) {}
