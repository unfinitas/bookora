package fi.unfinitas.bookora.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for login request.
 */
public record LoginRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
) {}
