package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.enums.BookingStatus;
import fi.unfinitas.bookora.domain.enums.UserRole;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import fi.unfinitas.bookora.domain.model.*;
import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;
import fi.unfinitas.bookora.dto.response.BookingResponse;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;
import fi.unfinitas.bookora.exception.CannotCancelBookingException;
import fi.unfinitas.bookora.mapper.BookingMapper;
import fi.unfinitas.bookora.repository.BookingRepository;
import fi.unfinitas.bookora.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Email Integration Tests")
class BookingServiceEmailIntegrationTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ServiceService serviceService;

    @Mock
    private GuestUserService guestUserService;

    @Mock
    private GuestAccessTokenService guestAccessTokenService;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookoraProperties bookoraProperties;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private CreateGuestBookingRequest validRequest;
    private Service service;
    private User guestUser;
    private Provider provider;
    private Booking booking;
    private GuestAccessToken token;
    private GuestBookingResponse guestBookingResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        final LocalDateTime startTime = LocalDateTime.now().plusDays(2);
        final LocalDateTime endTime = startTime.plusHours(1);

        validRequest = CreateGuestBookingRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("123456789")
            .serviceId(1L)
            .startTime(startTime)
            .endTime(endTime)
            .notes("Test booking")
            .build();

        guestUser = User.builder()
            .id(UUID.randomUUID())
            .email("john.doe@example.com")
            .firstName("John")
            .lastName("Doe")
            .isGuest(true)
            .role(UserRole.USER)
            .build();

        provider = Provider.builder()
            .id(UUID.randomUUID())
            .businessName("Test Salon")
            .build();

        service = Service.builder()
            .id(1L)
            .name("Haircut")
            .description("Professional haircut")
            .durationMinutes(60)
            .price(new BigDecimal("25.00"))
            .provider(provider)
            .build();

        booking = Booking.builder()
            .id(1L)
            .service(service)
            .customer(guestUser)
            .provider(provider)
            .startTime(startTime)
            .endTime(endTime)
            .status(BookingStatus.PENDING)
            .notes("Test booking")
            .build();

        token = GuestAccessToken.builder()
            .id(1L)
            .token(UUID.randomUUID())
            .booking(booking)
            .expiresAt(endTime)
            .build();

        guestBookingResponse = new GuestBookingResponse(
            1L,
            null, // serviceResponse
            "John Doe",
            "john.doe@example.com",
            "123456789",
            startTime,
            endTime,
            "PENDING",
            "Test booking",
            LocalDateTime.now(),
            token.getToken(),
            endTime
        );

        // Stub BookoraProperties for event publishing (lenient to avoid UnnecessaryStubbingException)
        lenient().when(bookoraProperties.getFrontendUrl()).thenReturn("http://localhost:3000");
    }

    @Test
    @DisplayName("createGuestBooking() - Valid request - Publishes SendMailEvent")
    void createGuestBooking_ValidRequest_PublishesSendMailEvent() {
        // GIVEN: Valid booking request
        when(serviceService.getServiceById(1L)).thenReturn(service);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(guestUserService.findOrCreateGuestUser(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(guestUser);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(guestAccessTokenService.generateToken(any(Booking.class))).thenReturn(token);
        when(bookingMapper.toGuestResponse(any(Booking.class), any(GuestAccessToken.class)))
            .thenReturn(guestBookingResponse);

        // WHEN: createGuestBooking is called
        bookingService.createGuestBooking(validRequest);

        // THEN: ApplicationEventPublisher.publishEvent is called
        final ArgumentCaptor<SendMailEvent> eventCaptor = ArgumentCaptor.forClass(SendMailEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        // AND: Event is SendMailEvent with BOOKING_CREATED template
        final SendMailEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.to()).isEqualTo("john.doe@example.com");
        assertThat(capturedEvent.subject()).contains("Booking Confirmation");
        assertThat(capturedEvent.templateName()).isEqualTo("email/booking-created");
        assertThat(capturedEvent.templateVariables()).containsKey("booking");
    }

    @Test
    @DisplayName("createGuestBooking() - Event publishing fails - Booking still created")
    void createGuestBooking_EventPublishingFails_BookingStillCreated() {
        // GIVEN: EventPublisher throws exception
        when(serviceService.getServiceById(1L)).thenReturn(service);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(guestUserService.findOrCreateGuestUser(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(guestUser);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(guestAccessTokenService.generateToken(any(Booking.class))).thenReturn(token);
        when(bookingMapper.toGuestResponse(any(Booking.class), any(GuestAccessToken.class)))
            .thenReturn(guestBookingResponse);

        doThrow(new RuntimeException("Event publishing failed"))
            .when(eventPublisher).publishEvent(any(SendMailEvent.class));

        // WHEN: createGuestBooking is called
        final GuestBookingResponse result = bookingService.createGuestBooking(validRequest);

        // THEN: Booking is still created
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);

        // AND: Exception is logged but not propagated
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(eventPublisher, times(1)).publishEvent(any(SendMailEvent.class));
    }

    @Test
    @DisplayName("cancelBookingByToken() - Valid token - Publishes SendMailEvent")
    void cancelBooking_ValidToken_PublishesSendMailEvent() {
        // GIVEN: Valid booking that can be cancelled
        final LocalDateTime startTime = LocalDateTime.now().plusDays(2);
        booking.setStartTime(startTime);
        booking.setStatus(BookingStatus.CONFIRMED);
        token.setBooking(booking);

        final BookingResponse bookingResponse = new BookingResponse(
            1L,
            null,
            "John Doe",
            "john.doe@example.com",
            "123456789",
            startTime,
            startTime.plusHours(1),
            "CANCELLED",
            "Test booking",
            LocalDateTime.now()
        );

        when(guestAccessTokenService.validateToken(any(UUID.class))).thenReturn(token);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(bookingResponse);

        // WHEN: cancelBookingByToken is called
        bookingService.cancelBookingByToken(token.getToken());

        // THEN: SendMailEvent is published with BOOKING_CANCELLED template
        final ArgumentCaptor<SendMailEvent> eventCaptor = ArgumentCaptor.forClass(SendMailEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        final SendMailEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.to()).isEqualTo("john.doe@example.com");
        assertThat(capturedEvent.subject()).contains("Booking Cancelled");
        assertThat(capturedEvent.templateName()).isEqualTo("email/booking-cancelled");
        assertThat(capturedEvent.templateVariables()).containsKey("booking");
    }

    @Test
    @DisplayName("cancelBookingByToken() - Event publishing fails - Booking still cancelled")
    void cancelBooking_EventPublishingFails_BookingStillCancelled() {
        // GIVEN: Valid booking and EventPublisher fails
        final LocalDateTime startTime = LocalDateTime.now().plusDays(2);
        booking.setStartTime(startTime);
        booking.setStatus(BookingStatus.CONFIRMED);
        token.setBooking(booking);

        final BookingResponse bookingResponse = new BookingResponse(
            1L,
            null,
            "John Doe",
            "john.doe@example.com",
            "123456789",
            startTime,
            startTime.plusHours(1),
            "CANCELLED",
            "Test booking",
            LocalDateTime.now()
        );

        when(guestAccessTokenService.validateToken(any(UUID.class))).thenReturn(token);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(bookingResponse);

        doThrow(new RuntimeException("Event publishing failed"))
            .when(eventPublisher).publishEvent(any(SendMailEvent.class));

        // WHEN: cancelBookingByToken is called
        final BookingResponse result = bookingService.cancelBookingByToken(token.getToken());

        // THEN: Booking is still cancelled
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("CANCELLED");

        // AND: Exception is logged but not propagated
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(eventPublisher, times(1)).publishEvent(any(SendMailEvent.class));
    }

    @Test
    @DisplayName("cancelBookingByToken() - Already cancelled - No event published")
    void cancelBooking_AlreadyCancelled_NoEventPublished() {
        // GIVEN: Booking already cancelled
        booking.setStatus(BookingStatus.CANCELLED);
        token.setBooking(booking);

        when(guestAccessTokenService.validateToken(any(UUID.class))).thenReturn(token);

        // WHEN: cancelBookingByToken is called
        // THEN: Throws CannotCancelBookingException
        assertThatThrownBy(() -> bookingService.cancelBookingByToken(token.getToken()))
            .isInstanceOf(CannotCancelBookingException.class);

        // AND: No event is published
        verify(eventPublisher, never()).publishEvent(any(SendMailEvent.class));
    }

    @Test
    @DisplayName("createGuestBooking() - Event contains correct template variables")
    void createGuestBooking_EventContainsCorrectVariables() {
        // GIVEN: Valid booking request
        when(serviceService.getServiceById(1L)).thenReturn(service);
        when(bookingRepository.existsOverlappingBooking(any(), any(), any())).thenReturn(false);
        when(guestUserService.findOrCreateGuestUser(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(guestUser);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(guestAccessTokenService.generateToken(any(Booking.class))).thenReturn(token);
        when(bookingMapper.toGuestResponse(any(Booking.class), any(GuestAccessToken.class)))
            .thenReturn(guestBookingResponse);

        // WHEN: createGuestBooking is called
        bookingService.createGuestBooking(validRequest);

        // THEN: Event contains correct template variables
        final ArgumentCaptor<SendMailEvent> eventCaptor = ArgumentCaptor.forClass(SendMailEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        final SendMailEvent event = eventCaptor.getValue();
        assertThat(event.templateVariables())
            .containsKey("booking")
            .containsKey("frontendUrl");
    }
}
