package fi.unfinitas.bookora.domain.model;

import fi.unfinitas.bookora.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static fi.unfinitas.bookora.domain.enums.UserRole.USER;

/**
 * User entity representing both regular users and service providers.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "t_user", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_email", columnNames = "email"),
        @UniqueConstraint(name = "uq_user_username", columnNames = "username")
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = USER;

    @Column(name = "is_guest", nullable = false)
    @Builder.Default
    private Boolean isGuest = false;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "last_verification_email_sent_at")
    private LocalDateTime lastVerificationEmailSentAt;

    @PrePersist
    @PreUpdate
    private void validate() {
        if (isGuest && password != null) {
            throw new IllegalStateException("Guest users cannot have password");
        }
        if (!isGuest && password == null) {
            throw new IllegalStateException("Registered users must have password");
        }
    }

    public boolean canResendVerificationEmail() {
        if (lastVerificationEmailSentAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lastVerificationEmailSentAt.plusHours(1));
    }
}
