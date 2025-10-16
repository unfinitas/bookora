package fi.unfinitas.bookora.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * User response for API responses (registration, etc.).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPublicInfo {

    private UUID id;
    private String username;
    private String role;
}
