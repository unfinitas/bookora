package fi.unfinitas.bookora.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "t_refresh_token")
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "token_family", nullable = false)
    private UUID tokenFamily;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "replaced_by_token_id")
    private Long replacedByTokenId;

    @Transient
    private String rawToken;  // Used only for returning the actual token to the user, not persisted

    /**
     * Check if the token is valid (not expired and not revoked).
     *
     * @return true if the token is valid
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }

    /**
     * Check if the token has expired.
     *
     * @return true if the token has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if the token has been revoked.
     *
     * @return true if the token has been revoked
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Revoke the token by setting the revocation timestamp.
     */
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Revoke the token and mark it as replaced by another token.
     *
     * @param replacedByTokenId the ID of the token that replaced this one
     */
    public void revokeAndReplace(Long replacedByTokenId) {
        this.revokedAt = LocalDateTime.now();
        this.replacedByTokenId = replacedByTokenId;
    }
}