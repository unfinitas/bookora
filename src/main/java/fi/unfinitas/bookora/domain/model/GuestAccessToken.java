package fi.unfinitas.bookora.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "t_guest_access_token")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE t_guest_access_token SET deleted_at = NOW(), updated_at = NOW() WHERE id = ? AND version = ?")
public class GuestAccessToken extends VersionedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private UUID token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "confirmed_at")
    @Builder.Default
    private LocalDateTime confirmedAt = null;

    /**
     * Check if the token is expired.
     *
     * @return true if the token has expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Mark the token as confirmed by setting the confirmedAt timestamp.
     * This is used for tracking when the guest first accessed their booking.
     * Does not prevent token reuse.
     */
    public void markAsConfirmed() {
        this.confirmedAt = LocalDateTime.now();
    }
}
