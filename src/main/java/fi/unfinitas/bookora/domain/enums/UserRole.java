package fi.unfinitas.bookora.domain.enums;

public enum UserRole {
    /**
     * Regular user who can make bookings
     */
    USER,

    /**
     * Service provider who can offer services and manage bookings
     */
    PROVIDER,

    /**
     * System administrator with full access
     */
    ADMIN
}
