package fi.unfinitas.bookora.domain.model;

import fi.unfinitas.bookora.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
        @UniqueConstraint(name = "uq_user_email", columnNames = "email")
})
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "is_guest", nullable = false)
    @Builder.Default
    private Boolean isGuest = false;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

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
}
