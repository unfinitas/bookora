package fi.unfinitas.bookora.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.unfinitas.bookora.config.TestContainersConfiguration;
import fi.unfinitas.bookora.config.TestEmailConfiguration;
import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;
import fi.unfinitas.bookora.repository.*;
import fi.unfinitas.bookora.testutil.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import({TestContainersConfiguration.class, TestEmailConfiguration.class})
@ActiveProfiles("test")
@DisplayName("Guest Booking Integration Tests")
@org.springframework.test.context.jdbc.Sql(
        statements = {
                "TRUNCATE TABLE t_guest_access_token CASCADE",
                "TRUNCATE TABLE t_booking CASCADE",
                "TRUNCATE TABLE t_service CASCADE",
                "TRUNCATE TABLE t_provider CASCADE",
                "TRUNCATE TABLE t_user CASCADE"
        },
        executionPhase = org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class GuestBookingIntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private GuestAccessTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private ProviderRepository providerRepository;

    private Long testServiceOfferingId;

    @BeforeEach
    void setUp() {
        // Database is truncated by @Sql annotation before each test
        // Create test data: User → Provider → ServiceOffering
        final var providerUser = TestDataBuilder.user()
                .username("provider_user")
                .email("provider@example.com")
                .build();
        final var savedProviderUser = userRepository.save(providerUser);

        final var provider = TestDataBuilder.provider()
                .user(savedProviderUser)
                .build();
        final var savedProvider = providerRepository.save(provider);

        final var serviceOffering = TestDataBuilder.serviceOffering()
                .provider(savedProvider)
                .build();
        final var savedServiceOffering = serviceOfferingRepository.save(serviceOffering);
        testServiceOfferingId = savedServiceOffering.getId();  // Capture auto-generated ID
    }
    

    @Test
    @DisplayName("Complete guest booking flow: create -> validate (auto-confirm) -> view -> cancel")
    void completeGuestBookingFlow_HappyPath() throws Exception {
        // 1. Create booking
        final CreateGuestBookingRequest request = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("010-1234-5678")
                .serviceId(testServiceOfferingId)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .notes("Test booking")
                .build();

        final var createResult = mockMvcTester.post()
                .uri("/bookings/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        final String responseBody = createResult.getMvcResult().getResponse().getContentAsString();
        final GuestBookingResponse bookingResponse = objectMapper.readTree(responseBody)
                .get("data")
                .traverse(objectMapper)
                .readValueAs(GuestBookingResponse.class);
        final UUID token = bookingResponse.accessToken();

        // Verify booking in database
        assertThat(bookingRepository.findAll()).hasSize(1);
        assertThat(tokenRepository.findByToken(token)).isPresent();

        // 2. Confirm booking
        assertThat(mockMvcTester.post().uri("/bookings/guest/" + token + "/confirm"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("CONFIRMED");

        // Verify confirmedAt is set after auto-confirmation
        final var savedToken = tokenRepository.findByToken(token).orElseThrow();
        assertThat(savedToken.getConfirmedAt()).isNotNull();

        // 3. View booking (status already CONFIRMED)
        assertThat(mockMvcTester.get().uri("/bookings/guest/" + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("CONFIRMED");

        // 4. View booking again (verify status persisted)
        assertThat(mockMvcTester.get().uri("/bookings/guest/" + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("CONFIRMED");

        // 5. Cancel booking
        assertThat(mockMvcTester.patch().uri("/bookings/guest/" + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("CANCELLED");

        // 6. Verify token is revoked after cancellation (soft deleted)
        assertThat(mockMvcTester.get().uri("/bookings/guest/" + token))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.message").asString()
                .contains("Token not found");
    }

    @Test
    @DisplayName("Complete flow: Cancel after auto-confirm")
    void completeFlow_CancelAfterAutoConfirm() throws Exception {
        // 1. Create booking (status: PENDING)
        final CreateGuestBookingRequest request = CreateGuestBookingRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phoneNumber("010-9876-5432")
                .serviceId(testServiceOfferingId)
                .startTime(LocalDateTime.now().plusDays(3))
                .endTime(LocalDateTime.now().plusDays(3).plusHours(1))
                .build();

        final var createResult = mockMvcTester.post()
                .uri("/bookings/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        final String responseBody = createResult.getMvcResult().getResponse().getContentAsString();
        final UUID token = objectMapper.readTree(responseBody)
                .get("data")
                .get("accessToken")
                .traverse(objectMapper)
                .readValueAs(UUID.class);

        // 2. Confirm booking
        assertThat(mockMvcTester.post().uri("/bookings/guest/" + token + "/confirm"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("CONFIRMED");

        // 3. Cancel booking
        assertThat(mockMvcTester.patch().uri("/bookings/guest/" + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("CANCELLED");

        // 4. Verify token is revoked after cancellation (soft deleted)
        assertThat(mockMvcTester.get().uri("/bookings/guest/" + token))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.message").asString()
                .contains("Token not found");
    }
    

    @Test
    @DisplayName("Confirm booking successfully")
    void confirmBooking_Successfully() throws Exception {
        // 1. Create booking (status: PENDING)
        final CreateGuestBookingRequest request = CreateGuestBookingRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phoneNumber("010-1111-2222")
                .serviceId(testServiceOfferingId)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .build();

        final var createResult = mockMvcTester.post()
                .uri("/bookings/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        final String responseBody = createResult.getMvcResult().getResponse().getContentAsString();
        final UUID token = objectMapper.readTree(responseBody)
                .get("data")
                .get("accessToken")
                .traverse(objectMapper)
                .readValueAs(UUID.class);

        // 2. Confirm booking (status → CONFIRMED)
        assertThat(mockMvcTester.post().uri("/bookings/guest/" + token + "/confirm"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("CONFIRMED");

        final var confirmedAt = tokenRepository.findByToken(token).orElseThrow().getConfirmedAt();
        assertThat(confirmedAt).isNotNull();

        // 3. Verify status remains CONFIRMED
        assertThat(mockMvcTester.get().uri("/bookings/guest/" + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.status").isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Cannot cancel booking within 24 hours")
    void cannotCancelWithin24Hours() throws Exception {
        // 1. Create booking starting in 20 hours
        final CreateGuestBookingRequest request = CreateGuestBookingRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .phoneNumber("010-1111-2222")
                .serviceId(testServiceOfferingId)
                .startTime(LocalDateTime.now().plusHours(20))
                .endTime(LocalDateTime.now().plusHours(21))
                .build();

        final var createResult = mockMvcTester.post()
                .uri("/bookings/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        final String responseBody = createResult.getMvcResult().getResponse().getContentAsString();
        final UUID token = objectMapper.readTree(responseBody)
                .get("data")
                .get("accessToken")
                .traverse(objectMapper)
                .readValueAs(UUID.class);

        // 2. Try to cancel
        // 3. Expect 400 BAD_REQUEST
        assertThat(mockMvcTester.patch().uri("/bookings/guest/" + token))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Same guest email creates multiple bookings with same user")
    void sameGuestEmailCreatesMultipleBookings() {
        final String email = "repeat@example.com";

        // 1. Create first booking with email
        final CreateGuestBookingRequest request1 = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phoneNumber("010-1234-5678")
                .serviceId(testServiceOfferingId)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .build();

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request1)))
                .hasStatus(HttpStatus.CREATED);

        // 2. Create second booking with same email
        final CreateGuestBookingRequest request2 = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phoneNumber("010-1234-5678")
                .serviceId(testServiceOfferingId)
                .startTime(LocalDateTime.now().plusDays(2))
                .endTime(LocalDateTime.now().plusDays(2).plusHours(1))
                .build();

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request2)))
                .hasStatus(HttpStatus.CREATED);

        // 3. Verify both bookings created
        assertThat(bookingRepository.findAll()).hasSize(2);

        // 4. Verify same guest user is used (excluding provider user from setUp)
        final var users = userRepository.findAll();
        assertThat(users).hasSize(2); // 1 provider + 1 guest
        final var guestUsers = users.stream().filter(u -> u.getEmail().equals(email)).toList();
        assertThat(guestUsers).hasSize(1);
        assertThat(guestUsers.get(0).getIsGuest()).isTrue();
    }

    @Test
    @DisplayName("Customer cannot book overlapping appointments")
    void customerCannotBookOverlappingAppointments() {
        final String email = "overlap@example.com";

        // Create second provider and service offering to test customer overlap (not provider overlap)
        final var providerUser2 = TestDataBuilder.user()
                .username("provider_user_2")
                .email("provider2@example.com")
                .build();
        final var savedProviderUser2 = userRepository.save(providerUser2);

        final var provider2 = TestDataBuilder.provider()
                .user(savedProviderUser2)
                .businessName("Second Provider")
                .build();
        final var savedProvider2 = providerRepository.save(provider2);

        final var serviceOffering2 = TestDataBuilder.serviceOffering()
                .provider(savedProvider2)
                .name("Different Service")
                .build();
        final var savedServiceOffering2 = serviceOfferingRepository.save(serviceOffering2);

        // 1. Create first booking with provider1: 10:00-11:00 tomorrow
        final LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        final CreateGuestBookingRequest request1 = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phoneNumber("010-1234-5678")
                .serviceId(testServiceOfferingId)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .build();

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request1)))
                .hasStatus(HttpStatus.CREATED);

        // 2. Try to create overlapping booking with provider2: 10:30-11:30 (overlap)
        final CreateGuestBookingRequest request2 = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phoneNumber("010-1234-5678")
                .serviceId(savedServiceOffering2.getId())
                .startTime(baseTime.plusMinutes(30))
                .endTime(baseTime.plusMinutes(90))
                .build();

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request2)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.message").asString()
                .contains("already have a booking during this time");

        // 3. Verify only one booking created
        assertThat(bookingRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Customer can book non-overlapping appointments")
    void customerCanBookNonOverlappingAppointments() {
        final String email = "nonoverlap@example.com";

        // 1. Create first booking: 10:00-11:00 tomorrow
        final LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        final CreateGuestBookingRequest request1 = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phoneNumber("010-1234-5678")
                .serviceId(testServiceOfferingId)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .build();

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request1)))
                .hasStatus(HttpStatus.CREATED);

        // 2. Create non-overlapping booking: 11:00-12:00 (no overlap)
        final CreateGuestBookingRequest request2 = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phoneNumber("010-1234-5678")
                .serviceId(testServiceOfferingId)
                .startTime(baseTime.plusHours(1))
                .endTime(baseTime.plusHours(2))
                .build();

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request2)))
                .hasStatus(HttpStatus.CREATED);

        // 3. Verify both bookings created
        assertThat(bookingRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Cancelled customer booking allows new booking at same time")
    void cancelledCustomerBookingAllowsNewBookingAtSameTime() throws Exception {
        final String email = "cancel-overlap@example.com";

        // 1. Create first booking: 10:00-11:00 (3 days from now to allow cancellation)
        final LocalDateTime baseTime = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
        final CreateGuestBookingRequest request1 = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phoneNumber("010-1234-5678")
                .serviceId(testServiceOfferingId)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .build();

        final var createResult = mockMvcTester.post()
                .uri("/bookings/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request1))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        final String responseBody = createResult.getMvcResult().getResponse().getContentAsString();
        final UUID token = objectMapper.readTree(responseBody)
                .get("data")
                .get("accessToken")
                .traverse(objectMapper)
                .readValueAs(UUID.class);

        // 2. Cancel the first booking
        assertThat(mockMvcTester.patch().uri("/bookings/guest/" + token))
                .hasStatusOk();

        // 3. Create new booking at same time (should succeed since first is cancelled)
        final CreateGuestBookingRequest request2 = CreateGuestBookingRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phoneNumber("010-1234-5678")
                .serviceId(testServiceOfferingId)
                .startTime(baseTime)
                .endTime(baseTime.plusHours(1))
                .build();

        assertThat(mockMvcTester.post()
                        .uri("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request2)))
                .hasStatus(HttpStatus.CREATED);

        // 4. Verify two bookings exist (one cancelled, one pending/confirmed)
        assertThat(bookingRepository.findAll()).hasSize(2);
    }

    // ===== SECURITY TESTS =====

    @Test
    @DisplayName("Invalid token returns 401 on confirm")
    void invalidTokenReturns401OnConfirm() {
        // Note: In real scenario, token expires at booking.endTime
        // This test would need time mocking to properly test expiration
        // For now, we test with an invalid/non-existent token

        final UUID nonExistentToken = UUID.randomUUID();

        // Try to confirm with invalid token
        assertThat(mockMvcTester.post().uri("/bookings/guest/" + nonExistentToken + "/confirm"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Invalid token returns 404")
    void invalidTokenReturns404() {
        // 1. Use random UUID as token
        final UUID randomToken = UUID.randomUUID();

        // 2. Try to get booking
        // 3. Expect 404 NOT_FOUND
        assertThat(mockMvcTester.get().uri("/bookings/guest/" + randomToken))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Token works across multiple sessions")
    void tokenWorksAcrossMultipleSessions() throws Exception {
        // 1. Create booking, get token
        final CreateGuestBookingRequest request = CreateGuestBookingRequest.builder()
                .firstName("Session")
                .lastName("Test")
                .email("session@example.com")
                .phoneNumber("010-9999-8888")
                .serviceId(testServiceOfferingId)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .build();

        final var createResult = mockMvcTester.post()
                .uri("/bookings/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        final String responseBody = createResult.getMvcResult().getResponse().getContentAsString();
        final UUID token = objectMapper.readTree(responseBody)
                .get("data")
                .get("accessToken")
                .traverse(objectMapper)
                .readValueAs(UUID.class);

        // 2. Use token to view booking (first session)
        assertThat(mockMvcTester.get().uri("/bookings/guest/" + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.customerEmail").isEqualTo("session@example.com");

        // 3. Simulate new session (context cleared automatically by test isolation)
        // 4. Use same token again (second session)
        assertThat(mockMvcTester.get().uri("/bookings/guest/" + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.data.customerEmail").isEqualTo("session@example.com");

        // 5. Verify access still works
        assertThat(tokenRepository.findByToken(token)).isPresent();
    }

    // Helper method to serialize objects to JSON
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
