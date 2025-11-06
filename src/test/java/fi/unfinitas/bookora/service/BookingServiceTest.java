package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.enums.BookingStatus;
import fi.unfinitas.bookora.domain.model.*;
import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;
import fi.unfinitas.bookora.dto.response.BookingResponse;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;
import fi.unfinitas.bookora.exception.*;
import fi.unfinitas.bookora.mapper.BookingMapper;
import fi.unfinitas.bookora.repository.BookingRepository;
import fi.unfinitas.bookora.service.impl.BookingServiceImpl;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceOfferingService serviceOfferingService;

    @Mock
    private GuestUserService guestUserService;

    @Mock
    private GuestAccessTokenService tokenService;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BookoraProperties bookoraProperties;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private CreateGuestBookingRequest validRequest;
    private ServiceOffering testServiceOffering;
    private User guestUser;
    private Booking testBooking;
    private GuestAccessToken testToken;
    private Provider testProvider;

    @BeforeEach
    void setUp() {
        validRequest = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phoneNumber("010-1234-5678")
                .serviceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .notes("Test booking")
                .build();

        testProvider = Provider.builder()
                .id(UUID.randomUUID())
                .businessName("Test Business")
                .build();

        testServiceOffering = ServiceOffering.builder()
                .id(1L)
                .provider(testProvider)
                .build();

        guestUser = User.builder()
                .email("john@example.com")
                .build();

        testBooking = Booking.builder()
                .id(1L)
                .customer(guestUser)
                .provider(testProvider)
                .serviceOffering(testServiceOffering)
                .startTime(validRequest.getStartTime())
                .endTime(validRequest.getEndTime())
                .status(BookingStatus.PENDING)
                .build();

        testToken = GuestAccessToken.builder()
                .token(UUID.randomUUID())
                .booking(testBooking)
                .build();

        // Stub BookoraProperties for event publishing and booking configuration (lenient to avoid UnnecessaryStubbingException)
        lenient().when(bookoraProperties.getFrontendUrl()).thenReturn("http://localhost:3000");

        // Mock nested properties for booking cancellation window
        final BookoraProperties.Guest guestProperties = mock(BookoraProperties.Guest.class);
        final BookoraProperties.Guest.Booking bookingProperties = mock(BookoraProperties.Guest.Booking.class);
        lenient().when(bookoraProperties.getGuest()).thenReturn(guestProperties);
        lenient().when(guestProperties.getBooking()).thenReturn(bookingProperties);
        lenient().when(bookingProperties.getCancellationWindowHours()).thenReturn(24);
    }

    @Test
    @DisplayName("Should create guest booking successfully")
    void shouldCreateGuestBookingSuccessfully() {
        when(serviceOfferingService.getServiceOfferingById(1L)).thenReturn(testServiceOffering);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(guestUserService.findOrCreateGuestUser(any(), any(), any(), any())).thenReturn(guestUser);
        when(bookingRepository.existsCustomerOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(tokenService.generateToken(any(Booking.class))).thenReturn(testToken);
        when(bookingMapper.toGuestResponse(any(Booking.class), any(GuestAccessToken.class)))
                .thenReturn(mock(GuestBookingResponse.class));

        final GuestBookingResponse result = bookingService.createGuestBooking(validRequest);

        assertThat(result).isNotNull();
        verify(serviceOfferingService).getServiceOfferingById(1L);
        verify(bookingRepository).existsOverlappingBooking(any(), any(), any());
        verify(guestUserService).findOrCreateGuestUser(
                validRequest.getEmail(),
                validRequest.getFirstName(),
                validRequest.getLastName(),
                validRequest.getPhoneNumber()
        );
        verify(bookingRepository).existsCustomerOverlappingBooking(any(), any(), any());
        verify(bookingRepository).save(any(Booking.class));
        verify(tokenService).generateToken(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw exception when service offering not found")
    void shouldThrowExceptionWhenServiceOfferingNotFound() {
        when(serviceOfferingService.getServiceOfferingById(1L))
                .thenThrow(new ServiceOfferingNotFoundException("Service offering not found with ID: 1"));

        assertThatThrownBy(() -> bookingService.createGuestBooking(validRequest))
                .isInstanceOf(ServiceOfferingNotFoundException.class)
                .hasMessageContaining("Service offering not found");

        verify(serviceOfferingService).getServiceOfferingById(1L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when time is in past")
    void shouldThrowExceptionWhenTimeIsInPast() {
        final CreateGuestBookingRequest pastRequest = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .serviceId(1L)
                .startTime(LocalDateTime.now().minusHours(1))
                .endTime(LocalDateTime.now().plusHours(1))
                .build();

        assertThatThrownBy(() -> bookingService.createGuestBooking(pastRequest))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("past");

        verify(serviceOfferingService, never()).getServiceOfferingById(any());
    }

    @Test
    @DisplayName("Should throw exception when end time is before start time")
    void shouldThrowExceptionWhenEndTimeBeforeStartTime() {
        final CreateGuestBookingRequest invalidRequest = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .serviceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusHours(12)) // End time before start time
                .build();

        assertThatThrownBy(() -> bookingService.createGuestBooking(invalidRequest))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("end time must be after start time");

        verify(serviceOfferingService, never()).getServiceOfferingById(any());
    }

    @Test
    @DisplayName("Should throw exception when end time equals start time")
    void shouldThrowExceptionWhenEndTimeEqualsStartTime() {
        final LocalDateTime sameTime = LocalDateTime.now().plusDays(1);
        final CreateGuestBookingRequest invalidRequest = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .serviceId(1L)
                .startTime(sameTime)
                .endTime(sameTime) // Same as start time
                .build();

        assertThatThrownBy(() -> bookingService.createGuestBooking(invalidRequest))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("end time must be after start time");

        verify(serviceOfferingService, never()).getServiceOfferingById(any());
    }

    @Test
    @DisplayName("Should throw exception when overlapping booking exists")
    void shouldThrowExceptionWhenOverlappingBookingExists() {
        when(serviceOfferingService.getServiceOfferingById(1L)).thenReturn(testServiceOffering);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createGuestBooking(validRequest))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("already booked");

        verify(bookingRepository).existsOverlappingBooking(any(), any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when customer has overlapping booking")
    void shouldThrowExceptionWhenCustomerHasOverlappingBooking() {
        when(serviceOfferingService.getServiceOfferingById(1L)).thenReturn(testServiceOffering);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(guestUserService.findOrCreateGuestUser(any(), any(), any(), any())).thenReturn(guestUser);
        when(bookingRepository.existsCustomerOverlappingBooking(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createGuestBooking(validRequest))
                .isInstanceOf(CustomerBookingConflictException.class)
                .hasMessageContaining("already have a booking during this time");

        verify(bookingRepository).existsOverlappingBooking(any(), any(), any());
        verify(guestUserService).findOrCreateGuestUser(any(), any(), any(), any());
        verify(bookingRepository).existsCustomerOverlappingBooking(any(), any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get booking by token successfully")
    void shouldGetBookingByTokenSuccessfully() {
        final UUID token = UUID.randomUUID();
        testToken.setBooking(testBooking);
        when(tokenService.validateToken(token)).thenReturn(testToken);
        when(bookingMapper.toResponse(testBooking)).thenReturn(mock(BookingResponse.class));

        final BookingResponse result = bookingService.getBookingByToken(token);

        assertThat(result).isNotNull();
        verify(tokenService).validateToken(token);
        verify(bookingMapper).toResponse(testBooking);
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token is not found")
    void shouldThrowInvalidTokenExceptionWhenTokenNotFound() {
        final UUID token = UUID.randomUUID();
        when(tokenService.validateToken(token))
                .thenThrow(new InvalidTokenException("Token not found"));

        assertThatThrownBy(() -> bookingService.getBookingByToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Token not found");

        verify(tokenService).validateToken(token);
        verify(bookingMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should throw TokenExpiredException when token is expired")
    void shouldThrowTokenExpiredExceptionWhenTokenExpired() {
        final UUID token = UUID.randomUUID();
        when(tokenService.validateToken(token))
                .thenThrow(new TokenExpiredException("Token expired"));

        assertThatThrownBy(() -> bookingService.getBookingByToken(token))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("Token expired");

        verify(tokenService).validateToken(token);
        verify(bookingMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Should cancel booking successfully")
    void shouldCancelBookingSuccessfully() {
        final UUID token = UUID.randomUUID();
        testBooking.setStartTime(LocalDateTime.now().plusDays(2));
        testToken.setBooking(testBooking);
        when(tokenService.validateToken(token)).thenReturn(testToken);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mock(BookingResponse.class));

        final BookingResponse result = bookingService.cancelBookingByToken(token);

        assertThat(result).isNotNull();
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(tokenService).validateToken(token);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("Should confirm booking successfully and mark token as confirmed")
    void shouldConfirmBookingSuccessfully() {
        final UUID token = UUID.randomUUID();
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setStartTime(LocalDateTime.now().plusDays(1));
        testToken.setBooking(testBooking);

        when(tokenService.validateTokenForConfirm(token)).thenReturn(testToken);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mock(BookingResponse.class));

        final BookingResponse result = bookingService.confirmBookingByToken(token);

        assertThat(result).isNotNull();
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(testToken.getConfirmedAt()).isNotNull(); // Verify token was marked as confirmed
        verify(tokenService).validateTokenForConfirm(token);
    }

    @Test
    @DisplayName("Should throw exception when confirming already confirmed booking")
    void shouldThrowExceptionWhenConfirmingAlreadyConfirmedBooking() {
        final UUID token = UUID.randomUUID();
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testToken.setBooking(testBooking);

        when(tokenService.validateTokenForConfirm(token)).thenReturn(testToken);

        assertThatThrownBy(() -> bookingService.confirmBookingByToken(token))
                .isInstanceOf(BookingAlreadyConfirmedException.class)
                .hasMessageContaining("Booking has already been confirmed");

        verify(tokenService).validateTokenForConfirm(token);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when confirming cancelled booking")
    void shouldThrowExceptionWhenConfirmingCancelledBooking() {
        final UUID token = UUID.randomUUID();
        testBooking.setStatus(BookingStatus.CANCELLED);
        testToken.setBooking(testBooking);

        when(tokenService.validateTokenForConfirm(token)).thenReturn(testToken);

        assertThatThrownBy(() -> bookingService.confirmBookingByToken(token))
                .isInstanceOf(BookingAlreadyCancelledException.class)
                .hasMessageContaining("cancelled");

        verify(tokenService).validateTokenForConfirm(token);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when confirming booking that already started")
    void shouldThrowExceptionWhenConfirmingBookingThatAlreadyStarted() {
        final UUID token = UUID.randomUUID();
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setStartTime(LocalDateTime.now().minusHours(1));
        testToken.setBooking(testBooking);

        when(tokenService.validateTokenForConfirm(token)).thenReturn(testToken);

        assertThatThrownBy(() -> bookingService.confirmBookingByToken(token))
                .isInstanceOf(InvalidBookingTimeException.class)
                .hasMessageContaining("already started");

        verify(tokenService).validateTokenForConfirm(token);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when cancelling booking within 24 hours")
    void shouldThrowExceptionWhenCancellingBookingWithin24Hours() {
        final UUID token = UUID.randomUUID();
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setStartTime(LocalDateTime.now().plusHours(23)); // Less than 24 hours
        testToken.setBooking(testBooking);

        when(tokenService.validateToken(token)).thenReturn(testToken);

        assertThatThrownBy(() -> bookingService.cancelBookingByToken(token))
                .isInstanceOf(CannotCancelBookingException.class)
                .hasMessageContaining("24 hours");

        verify(tokenService).validateToken(token);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled booking")
    void shouldThrowExceptionWhenCancellingAlreadyCancelledBooking() {
        final UUID token = UUID.randomUUID();
        testBooking.setStatus(BookingStatus.CANCELLED);
        testToken.setBooking(testBooking);

        when(tokenService.validateToken(token)).thenReturn(testToken);

        assertThatThrownBy(() -> bookingService.cancelBookingByToken(token))
                .isInstanceOf(CannotCancelBookingException.class)
                .hasMessageContaining("Booking has already been cancelled");

        verify(tokenService).validateToken(token);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow cancelling confirmed booking more than 24 hours before start")
    void shouldAllowCancellingConfirmedBooking() {
        final UUID token = UUID.randomUUID();
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setStartTime(LocalDateTime.now().plusDays(2)); // More than 24 hours
        testToken.setBooking(testBooking);

        when(tokenService.validateToken(token)).thenReturn(testToken);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mock(BookingResponse.class));

        final BookingResponse result = bookingService.cancelBookingByToken(token);

        assertThat(result).isNotNull();
        assertThat(testBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(tokenService).validateToken(token);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("Should propagate OptimisticLockException when modifying booking with stale version")
    void shouldPropagateOptimisticLockExceptionWhenCancellingBooking() {
        final UUID token = UUID.randomUUID();
        testBooking.setStatus(BookingStatus.PENDING);
        testBooking.setStartTime(LocalDateTime.now().plusDays(2)); // More than 24 hours
        testToken.setBooking(testBooking);

        when(tokenService.validateToken(token)).thenReturn(testToken);
        when(bookoraProperties.getGuest()).thenReturn(mock(BookoraProperties.Guest.class));
        when(bookoraProperties.getGuest().getBooking()).thenReturn(mock(BookoraProperties.Guest.Booking.class));
        when(bookoraProperties.getGuest().getBooking().getCancellationWindowHours()).thenReturn(24);
        when(bookingRepository.save(any(Booking.class))).thenThrow(
                new OptimisticLockException("Booking was modified by another transaction")
        );

        assertThatThrownBy(() -> bookingService.cancelBookingByToken(token))
                .isInstanceOf(OptimisticLockException.class)
                .hasMessageContaining("modified by another transaction");

        verify(tokenService).validateToken(token);
        verify(bookingRepository).save(any(Booking.class));
    }
}
