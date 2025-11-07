package fi.unfinitas.bookora.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.config.WebConfig;
import fi.unfinitas.bookora.dto.request.ResendVerificationRequest;
import fi.unfinitas.bookora.exception.GlobalExceptionHandler;
import fi.unfinitas.bookora.exception.RateLimitExceededException;
import fi.unfinitas.bookora.exception.UserNotFoundException;
import fi.unfinitas.bookora.exception.VerificationTokenExpiredException;
import fi.unfinitas.bookora.exception.VerificationTokenInvalidException;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.EmailVerificationService;
import fi.unfinitas.bookora.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest({EmailVerificationController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({BookoraProperties.class, WebConfig.class})
class EmailVerificationControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BookoraProperties bookoraProperties;

    @BeforeEach
    void setUp() {
        when(bookoraProperties.getFrontendUrl()).thenReturn("http://localhost:3000");
    }

    @Test
    @DisplayName("Should successfully verify email and redirect to frontend")
    @WithMockUser
    void shouldSuccessfullyVerifyEmailAndRedirectToFrontend() throws Exception {
        final UUID token = UUID.randomUUID();
        doNothing().when(emailVerificationService).verifyEmail(token);

        assertThat(mockMvcTester.perform(get("/auth/verify/" + token)))
                .hasStatus(HttpStatus.FOUND)
                .hasHeader("Location", "http://localhost:3000?verified=true");

        verify(emailVerificationService).verifyEmail(token);
    }

    @Test
    @DisplayName("Should redirect with error when token is invalid")
    @WithMockUser
    void shouldRedirectWithErrorWhenTokenIsInvalid() throws Exception {
        final UUID token = UUID.randomUUID();
        doThrow(new VerificationTokenInvalidException("Invalid verification token"))
                .when(emailVerificationService).verifyEmail(token);

        assertThat(mockMvcTester.perform(get("/auth/verify/" + token)))
                .hasStatus(HttpStatus.FOUND)
                .satisfies(result -> {
                    final String location = result.getResponse().getHeader("Location");
                    assertThat(location)
                            .startsWith("http://localhost:3000")
                            .contains("verified=false");
                });

        verify(emailVerificationService).verifyEmail(token);
    }

    @Test
    @DisplayName("Should redirect with error when token is expired")
    @WithMockUser
    void shouldRedirectWithErrorWhenTokenIsExpired() throws Exception {
        final UUID token = UUID.randomUUID();
        doThrow(new VerificationTokenExpiredException("Verification token has expired"))
                .when(emailVerificationService).verifyEmail(token);

        assertThat(mockMvcTester.perform(get("/auth/verify/" + token)))
                .hasStatus(HttpStatus.FOUND)
                .satisfies(result -> {
                    final String location = result.getResponse().getHeader("Location");
                    assertThat(location)
                            .startsWith("http://localhost:3000")
                            .contains("verified=false");
                });

        verify(emailVerificationService).verifyEmail(token);
    }

    @Test
    @DisplayName("Should redirect with error when user not found")
    @WithMockUser
    void shouldRedirectWithErrorWhenUserNotFound() throws Exception {
        final UUID token = UUID.randomUUID();
        doThrow(new UserNotFoundException("User not found"))
                .when(emailVerificationService).verifyEmail(token);

        assertThat(mockMvcTester.perform(get("/auth/verify/" + token)))
                .hasStatus(HttpStatus.FOUND)
                .satisfies(result -> {
                    final String location = result.getResponse().getHeader("Location");
                    assertThat(location)
                            .startsWith("http://localhost:3000")
                            .contains("verified=false");
                });

        verify(emailVerificationService).verifyEmail(token);
    }

    @Test
    @DisplayName("Should successfully resend verification email")
    @WithMockUser
    void shouldSuccessfullyResendVerificationEmail() throws Exception {
        final ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
        doNothing().when(emailVerificationService).resendVerificationEmail(request.email());

        assertThat(mockMvcTester.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.message", message -> assertThat(message).isEqualTo("Verification email sent successfully"))
                .hasPathSatisfying("$.data", data -> assertThat(data).isEqualTo("Please check your email for the verification link"));

        verify(emailVerificationService).resendVerificationEmail(request.email());
    }

    @Test
    @DisplayName("Should return 400 when resend request email is invalid")
    @WithMockUser
    void shouldReturn400WhenResendRequestEmailIsInvalid() throws Exception {
        final ResendVerificationRequest invalidRequest = new ResendVerificationRequest("invalid-email");

        assertThat(mockMvcTester.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(emailVerificationService, never()).resendVerificationEmail(any());
    }

    @Test
    @DisplayName("Should return 400 when resend request email is blank")
    @WithMockUser
    void shouldReturn400WhenResendRequestEmailIsBlank() throws Exception {
        final ResendVerificationRequest invalidRequest = new ResendVerificationRequest("");

        assertThat(mockMvcTester.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(emailVerificationService, never()).resendVerificationEmail(any());
    }

    @Test
    @DisplayName("Should return 400 when resend request email is null")
    @WithMockUser
    void shouldReturn400WhenResendRequestEmailIsNull() throws Exception {
        final String invalidJson = "{\"email\": null}";

        assertThat(mockMvcTester.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(emailVerificationService, never()).resendVerificationEmail(any());
    }

    @Test
    @DisplayName("Should return 429 when rate limit exceeded")
    @WithMockUser
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        final ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
        doThrow(new RateLimitExceededException("Please wait at least 1 hour before requesting another verification email"))
                .when(emailVerificationService).resendVerificationEmail(request.email());

        assertThat(mockMvcTester.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        verify(emailVerificationService).resendVerificationEmail(request.email());
    }

    @Test
    @DisplayName("Should return 404 when user not found for resend")
    @WithMockUser
    void shouldReturn404WhenUserNotFoundForResend() throws Exception {
        final ResendVerificationRequest request = new ResendVerificationRequest("nonexistent@example.com");
        doThrow(new UserNotFoundException("User not found with email: nonexistent@example.com"))
                .when(emailVerificationService).resendVerificationEmail(request.email());

        assertThat(mockMvcTester.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .hasStatus(HttpStatus.NOT_FOUND);

        verify(emailVerificationService).resendVerificationEmail(request.email());
    }

    @Test
    @DisplayName("Should verify email endpoint is publicly accessible")
    void shouldVerifyEmailEndpointIsPubliclyAccessible() throws Exception {
        final UUID token = UUID.randomUUID();
        doNothing().when(emailVerificationService).verifyEmail(token);

        // Test without @WithMockUser to verify public access
        assertThat(mockMvcTester.perform(get("/auth/verify/" + token)))
                .hasStatus(HttpStatus.FOUND)
                .hasHeader("Location", "http://localhost:3000?verified=true");

        verify(emailVerificationService).verifyEmail(token);
    }

    @Test
    @DisplayName("Should resend verification endpoint is publicly accessible")
    void shouldResendVerificationEndpointIsPubliclyAccessible() throws Exception {
        final ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");
        doNothing().when(emailVerificationService).resendVerificationEmail(request.email());

        // Test without @WithMockUser to verify public access
        assertThat(mockMvcTester.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))))
                .hasStatusOk();

        verify(emailVerificationService).resendVerificationEmail(request.email());
    }
}
