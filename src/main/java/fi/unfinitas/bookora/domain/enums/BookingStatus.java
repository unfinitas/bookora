package fi.unfinitas.bookora.domain.enums;

public enum BookingStatus {
    /**
     * Booking has been created but not yet confirmed
     */
    PENDING,

    /**
     * Booking has been confirmed by the provider
     */
    CONFIRMED,

    /**
     * Booking has been cancelled
     */
    CANCELLED,

    /**
     * Booking has been completed
     */
    COMPLETED
}
