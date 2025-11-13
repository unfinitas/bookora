package fi.unfinitas.bookora.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Address entity representing physical addresses.
 * Separate table for reusability, historical tracking, and efficient querying.
 */
@Entity
@Table(name = "t_address")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "street")
    private String street;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state_province")
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "country", nullable = false)
    private String country;
}
