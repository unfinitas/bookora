package fi.unfinitas.bookora.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ResetPasswordDto(
        @NotNull(message = "Token is required")
        UUID token,

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,100}$",
                message = "Password must be 8-100 characters with at least one uppercase, lowercase, digit, and special character"
        )
        String newPassword
) {
}
