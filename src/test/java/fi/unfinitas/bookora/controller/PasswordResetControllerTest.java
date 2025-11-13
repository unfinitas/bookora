package fi.unfinitas.bookora.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.config.WebConfig;
import fi.unfinitas.bookora.dto.request.ForgotPasswordRequest;
import fi.unfinitas.bookora.dto.request.ResetPasswordDto;
import fi.unfinitas.bookora.exception.GlobalExceptionHandler;
import fi.unfinitas.bookora.exception.PasswordResetTokenAlreadyUsedException;
import fi.unfinitas.bookora.exception.PasswordResetTokenExpiredException;
import fi.unfinitas.bookora.exception.PasswordResetTokenInvalidException;
import fi.unfinitas.bookora.exception.RateLimitExceededException;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest({PasswordResetController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@Import({BookoraProperties.class, WebConfig.class})
class PasswordResetControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ForgotPasswordRequest validForgotPasswordRequest;
    private ResetPasswordDto validResetPasswordDto;
    private UUID validToken;

    @BeforeEach
    void setUp() {
        validForgotPasswordRequest = new ForgotPasswordRequest("test@example.com");
        validToken = UUID.randomUUID();
        validResetPasswordDto = new ResetPasswordDto(validToken, "NewSecure123!");
    }

    @Test
    void shouldSuccessfullyRequestPasswordReset() throws Exception {
        doNothing().when(passwordResetService).requestPasswordReset(any(ForgotPasswordRequest.class));

        assertThat(mockMvcTester.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validForgotPasswordRequest))))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.message", message -> assertThat(message)
                        .isEqualTo("If an account exists with this email, a password reset link has been sent."));

        verify(passwordResetService).requestPasswordReset(any(ForgotPasswordRequest.class));
    }

    @Test
    void shouldReturn400WhenRequestEmailIsInvalid() throws Exception {
        final ForgotPasswordRequest invalidRequest = new ForgotPasswordRequest("invalid-email");

        assertThat(mockMvcTester.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).requestPasswordReset(any());
    }

    @Test
    void shouldReturn400WhenRequestEmailIsBlank() throws Exception {
        final ForgotPasswordRequest invalidRequest = new ForgotPasswordRequest("");

        assertThat(mockMvcTester.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).requestPasswordReset(any());
    }

    @Test
    void shouldReturn400WhenRequestEmailIsNull() throws Exception {
        final String invalidJson = "{\"email\": null}";

        assertThat(mockMvcTester.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).requestPasswordReset(any());
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        doThrow(new RateLimitExceededException("Please wait before requesting another password reset email"))
                .when(passwordResetService).requestPasswordReset(any(ForgotPasswordRequest.class));

        assertThat(mockMvcTester.perform(post("/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validForgotPasswordRequest))))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);

        verify(passwordResetService).requestPasswordReset(any(ForgotPasswordRequest.class));
    }

    @Test
    void shouldSuccessfullyResetPassword() throws Exception {
        doNothing().when(passwordResetService).resetPassword(any(ResetPasswordDto.class));

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetPasswordDto))))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo("SUCCESS"))
                .hasPathSatisfying("$.message", message -> assertThat(message)
                        .isEqualTo("Your password has been successfully reset."));

        verify(passwordResetService).resetPassword(any(ResetPasswordDto.class));
    }

    @Test
    void shouldReturn400WhenResetTokenIsInvalid() throws Exception {
        doThrow(new PasswordResetTokenInvalidException("Invalid password reset token"))
                .when(passwordResetService).resetPassword(any(ResetPasswordDto.class));

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetPasswordDto))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService).resetPassword(any(ResetPasswordDto.class));
    }

    @Test
    void shouldReturn400WhenResetTokenIsExpired() throws Exception {
        doThrow(new PasswordResetTokenExpiredException("Password reset token has expired"))
                .when(passwordResetService).resetPassword(any(ResetPasswordDto.class));

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetPasswordDto))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService).resetPassword(any(ResetPasswordDto.class));
    }

    @Test
    void shouldReturn403WhenResetTokenIsAlreadyUsed() throws Exception {
        doThrow(new PasswordResetTokenAlreadyUsedException("This password reset token has already been used"))
                .when(passwordResetService).resetPassword(any(ResetPasswordDto.class));

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validResetPasswordDto))))
                .hasStatus(HttpStatus.FORBIDDEN);

        verify(passwordResetService).resetPassword(any(ResetPasswordDto.class));
    }

    @Test
    void shouldReturn400WhenResetPasswordIsWeak() throws Exception {
        final ResetPasswordDto weakPasswordRequest = new ResetPasswordDto(validToken, "weak");

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weakPasswordRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).resetPassword(any());
    }

    @Test
    void shouldReturn400WhenResetPasswordIsMissingUppercase() throws Exception {
        final ResetPasswordDto invalidPasswordRequest = new ResetPasswordDto(validToken, "newsecure123!");

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).resetPassword(any());
    }

    @Test
    void shouldReturn400WhenResetPasswordIsMissingLowercase() throws Exception {
        final ResetPasswordDto invalidPasswordRequest = new ResetPasswordDto(validToken, "NEWSECURE123!");

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).resetPassword(any());
    }

    @Test
    void shouldReturn400WhenResetPasswordIsMissingDigit() throws Exception {
        final ResetPasswordDto invalidPasswordRequest = new ResetPasswordDto(validToken, "NewSecure!");

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).resetPassword(any());
    }

    @Test
    void shouldReturn400WhenResetPasswordIsMissingSpecialChar() throws Exception {
        final ResetPasswordDto invalidPasswordRequest = new ResetPasswordDto(validToken, "NewSecure123");

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).resetPassword(any());
    }

    @Test
    void shouldReturn400WhenResetPasswordIsBlank() throws Exception {
        final ResetPasswordDto invalidPasswordRequest = new ResetPasswordDto(validToken, "");

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPasswordRequest))))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).resetPassword(any());
    }

    @Test
    void shouldReturn400WhenResetTokenIsNull() throws Exception {
        final String invalidJson = "{\"token\": null, \"newPassword\": \"NewSecure123!\"}";

        assertThat(mockMvcTester.perform(post("/auth/password-reset/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verify(passwordResetService, never()).resetPassword(any());
    }
}
