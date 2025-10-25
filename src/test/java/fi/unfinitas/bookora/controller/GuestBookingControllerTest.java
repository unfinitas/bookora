package fi.unfinitas.bookora.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.unfinitas.bookora.config.security.JwtAuthenticationFilter;
import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;
import fi.unfinitas.bookora.dto.response.BookingResponse;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;
import fi.unfinitas.bookora.dto.response.ServiceResponse;
import fi.unfinitas.bookora.exception.InvalidTokenException;
import fi.unfinitas.bookora.exception.ServiceNotFoundException;
import fi.unfinitas.bookora.exception.TokenExpiredException;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@WebMvcTest(GuestBookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuestBookingControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BookingService bookingService;

    private CreateGuestBookingRequest validRequest;
    private GuestBookingResponse guestBookingResponse;
    private BookingResponse bookingResponse;
    private UUID testToken;

    @BeforeEach
    void setUp() {
        testToken = UUID.randomUUID();

        validRequest = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("010-1234-5678")
                .serviceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .notes("Test booking")
                .build();

        final ServiceResponse serviceResponse = new ServiceResponse(
                1L,
                "Haircut",
                "Professional haircut service",
                60,
                BigDecimal.valueOf(30000),
                "John's Salon"
        );

        guestBookingResponse = new GuestBookingResponse(
                1L,
                serviceResponse,
                "John Doe",
                "john.doe@example.com",
                "010-1234-5678",
                validRequest.getStartTime(),
                validRequest.getEndTime(),
                "PENDING",
                "Test booking",
                LocalDateTime.now(),
                testToken,
                LocalDateTime.now().plusHours(24)
        );

        bookingResponse = new BookingResponse(
                1L,
                serviceResponse,
                "John Doe",
                "john.doe@example.com",
                "010-1234-5678",
                validRequest.getStartTime(),
                validRequest.getEndTime(),
                "PENDING",
                "Test booking",
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should create guest booking successfully")
    void shouldCreateGuestBookingSuccessfully() throws Exception {
        when(bookingService.createGuestBooking(any(CreateGuestBookingRequest.class)))
                .thenReturn(guestBookingResponse);

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.data.accessToken", token -> assertThat(token).isNotNull())
                .hasPathSatisfying("$.data.customerName", name -> assertThat(name).isEqualTo("John Doe"));

        verify(bookingService).createGuestBooking(any(CreateGuestBookingRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when request is invalid")
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        final CreateGuestBookingRequest invalidRequest = CreateGuestBookingRequest.builder()
                .firstName("")
                .lastName("")
                .email("invalid-email")
                .serviceId(1L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .build();

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(bookingService, never()).createGuestBooking(any(CreateGuestBookingRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when service not found")
    void shouldReturn404WhenServiceNotFound() throws Exception {
        when(bookingService.createGuestBooking(any(CreateGuestBookingRequest.class)))
                .thenThrow(new ServiceNotFoundException("Service not found"));

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .hasStatus(HttpStatus.NOT_FOUND);

        verify(bookingService).createGuestBooking(any(CreateGuestBookingRequest.class));
    }

    @Test
    @DisplayName("Should get booking by token successfully")
    void shouldGetBookingByTokenSuccessfully()  {
        when(bookingService.getBookingByToken(testToken)).thenReturn(bookingResponse);

        assertThat(mockMvcTester.get().uri("/bookings/guest/{token}", testToken))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.data.customerName", name -> assertThat(name).isEqualTo("John Doe"));

        verify(bookingService).getBookingByToken(testToken);
    }

    @Test
    @DisplayName("Should return 401 when token is expired")
    void shouldReturn401WhenTokenIsExpired()  {
        when(bookingService.getBookingByToken(testToken))
                .thenThrow(new TokenExpiredException("Token has expired"));

        assertThat(mockMvcTester.get().uri("/bookings/guest/{token}", testToken))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        verify(bookingService).getBookingByToken(testToken);
    }

    @Test
    @DisplayName("Should cancel booking successfully")
    void shouldCancelBookingSuccessfully() {
        final BookingResponse cancelledResponse = new BookingResponse(
                1L,
                bookingResponse.service(),
                "John Doe",
                "john.doe@example.com",
                "010-1234-5678",
                validRequest.getStartTime(),
                validRequest.getEndTime(),
                "CANCELLED",
                "Test booking",
                LocalDateTime.now()
        );

        when(bookingService.cancelBookingByToken(testToken)).thenReturn(cancelledResponse);

        assertThat(mockMvcTester.delete().uri("/bookings/guest/{token}", testToken))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.data.status", bookingStatus -> assertThat(bookingStatus).isEqualTo("CANCELLED"));

        verify(bookingService).cancelBookingByToken(testToken);
    }
}
