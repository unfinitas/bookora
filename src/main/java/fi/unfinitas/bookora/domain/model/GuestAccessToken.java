package fi.unfinitas.bookora.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * GuestAccessToken entity for providing temporary access to bookings for guest users.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "t_guest_access_token", uniqueConstraints = {
        @UniqueConstraint(name = "uq_guest_access_token_token", columnNames = "token"),
        @UniqueConstraint(name = "uq_guest_access_token_booking_id", columnNames = "booking_id")
})
public class GuestAccessToken extends BaseEntity {

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
     * Check if the token is valid (not expired).
     * Note: confirmedAt does not affect validity - tokens can be reused.
     *
     * @return true if the token has not expired
     */
    public boolean isValid() {
        return !isExpired();
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
