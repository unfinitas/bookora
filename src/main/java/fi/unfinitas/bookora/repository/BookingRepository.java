package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find all bookings for a specific customer.
     *
     * @param customerId the customer's user ID
     * @return list of bookings for the customer
     */
    List<Booking> findByCustomerId(UUID customerId);

    /**
     * Find all bookings for a specific provider.
     *
     * @param providerId the provider's ID
     * @return list of bookings for the provider
     */
    List<Booking> findByProviderId(UUID providerId);

    /**
     * Check if there are any overlapping bookings for a provider in the given time range.
     * Only considers bookings with status PENDING or CONFIRMED.
     *
     * @param providerId the provider's ID
     * @param startTime  the start time of the new booking
     * @param endTime    the end time of the new booking
     * @return true if there is an overlapping booking, false otherwise
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.provider.id = :providerId " +
            "AND b.deletedAt IS NULL " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    boolean existsOverlappingBooking(
            @Param("providerId") UUID providerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Check if there are any overlapping bookings for a customer in the given time range.
     * Only considers bookings with status PENDING or CONFIRMED.
     * This prevents a customer from booking multiple appointments at the same time.
     *
     * @param customerId the customer's user ID
     * @param startTime  the start time of the new booking
     * @param endTime    the end time of the new booking
     * @return true if there is an overlapping booking, false otherwise
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.customer.id = :customerId " +
            "AND b.deletedAt IS NULL " +
            "AND b.status IN ('PENDING', 'CONFIRMED') " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    boolean existsCustomerOverlappingBooking(
            @Param("customerId") UUID customerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
