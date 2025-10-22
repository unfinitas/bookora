package fi.unfinitas.bookora.testutil;

import fi.unfinitas.bookora.domain.enums.BookingStatus;
import fi.unfinitas.bookora.domain.enums.UserRole;
import fi.unfinitas.bookora.domain.model.*;
import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TestDataBuilder {

    public static CreateGuestBookingRequest.CreateGuestBookingRequestBuilder guestBookingRequest() {
        return CreateGuestBookingRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("123456789")
            .serviceId(1L)
            .startTime(LocalDateTime.now().plusDays(2))
            .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
            .notes("Test booking");
    }

    public static Service.ServiceBuilder service() {
        return Service.builder()
            .name("Haircut")
            .description("Professional haircut")
            .durationMinutes(60)
            .price(new BigDecimal("25.00"));
    }

    public static Provider.ProviderBuilder provider() {
        return Provider.builder()
            .businessName("Test Salon");
    }

    public static User.UserBuilder user() {
        return User.builder()
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .password("password123")  // Required for registered users
            .role(UserRole.USER)
            .isGuest(false)
            .isEmailVerified(true);
    }

    public static User.UserBuilder guestUser() {
        return User.builder()
            .username("guestuser")
            .firstName("Guest")
            .lastName("User")
            .email("guest@example.com")
            .password(null)  // Guest users don't have passwords
            .role(UserRole.USER)
            .isGuest(true)
            .isEmailVerified(true);
    }

    public static Booking.BookingBuilder booking() {
        return Booking.builder()
            .startTime(LocalDateTime.now().plusDays(2))
            .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
            .status(BookingStatus.PENDING);
    }

    public static GuestAccessToken.GuestAccessTokenBuilder guestAccessToken() {
        return GuestAccessToken.builder()
            .token(UUID.randomUUID())
            .expiresAt(LocalDateTime.now().plusDays(2).plusHours(1));
    }
}
