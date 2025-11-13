package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.model.PasswordResetToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.ForgotPasswordRequest;
import fi.unfinitas.bookora.dto.request.ResetPasswordDto;
import fi.unfinitas.bookora.exception.*;
import fi.unfinitas.bookora.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BookoraProperties bookoraProperties;

    @Mock
    private BookoraProperties.PasswordReset passwordReset;

    @Mock
    private BookoraProperties.PasswordReset.Token tokenConfig;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private PasswordResetToken testToken;
    private UUID tokenUUID;
    private final int expirationHours = 1;
    private final String newPassword = "NewSecure123!";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("oldEncodedPassword")
                .isGuest(false)
                .isEmailVerified(true)
                .lastPasswordResetEmailSentAt(null)
                .build();

        tokenUUID = UUID.randomUUID();

        testToken = PasswordResetToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token(tokenUUID)
                .expiresAt(LocalDateTime.now().plusHours(expirationHours))
                .usedAt(null)
                .build();

        lenient().when(bookoraProperties.getPasswordReset()).thenReturn(passwordReset);
        lenient().when(passwordReset.getToken()).thenReturn(tokenConfig);
        lenient().when(tokenConfig.getExpirationHours()).thenReturn(expirationHours);
        lenient().when(bookoraProperties.getFrontendUrl()).thenReturn("http://localhost:3000");
    }

    @Test
    void requestPasswordReset_Success() {
        when(userService.findByEmail(testUser.getEmail())).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestPasswordReset(new ForgotPasswordRequest(testUser.getEmail()));

        verify(userService).findByEmail(testUser.getEmail());
        verify(tokenRepository).deleteByUserId(testUser.getId());
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(eventPublisher).publishEvent(any(fi.unfinitas.bookora.domain.event.SendMailEvent.class));
        verify(userService).updateLastPasswordResetEmailSentAt(testUser.getId());
    }

    @Test
    void requestPasswordReset_UserNotFound() {
        when(userService.findByEmail(testUser.getEmail())).thenThrow(new UserNotFoundException("User not found with email: " + testUser.getEmail()));

        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(new ForgotPasswordRequest(testUser.getEmail())))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with email");

        verify(userService).findByEmail(testUser.getEmail());
        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void requestPasswordReset_RateLimitExceeded() {
        testUser.setLastPasswordResetEmailSentAt(LocalDateTime.now().minusMinutes(30));

        when(userService.findByEmail(testUser.getEmail())).thenReturn(testUser);

        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(new ForgotPasswordRequest(testUser.getEmail())))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Please wait before requesting another password reset email");

        verify(userService).findByEmail(testUser.getEmail());
        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void requestPasswordReset_AllowedAfter1Hour() {
        testUser.setLastPasswordResetEmailSentAt(LocalDateTime.now().minusHours(2));

        when(userService.findByEmail(testUser.getEmail())).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestPasswordReset(new ForgotPasswordRequest(testUser.getEmail()));

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(eventPublisher).publishEvent(any(fi.unfinitas.bookora.domain.event.SendMailEvent.class));
    }

    @Test
    void requestPasswordReset_AllowedWhenLastPasswordResetEmailSentAtIsNull() {
        testUser.setLastPasswordResetEmailSentAt(null);

        when(userService.findByEmail(testUser.getEmail())).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestPasswordReset(new ForgotPasswordRequest(testUser.getEmail()));

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(eventPublisher).publishEvent(any(fi.unfinitas.bookora.domain.event.SendMailEvent.class));
    }

    @Test
    void requestPasswordReset_CorrectExpirationTime() {
        when(userService.findByEmail(testUser.getEmail())).thenReturn(testUser);
        final ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestPasswordReset(new ForgotPasswordRequest(testUser.getEmail()));

        verify(tokenRepository).save(tokenCaptor.capture());
        final PasswordResetToken savedToken = tokenCaptor.getValue();

        final LocalDateTime expectedExpiration = LocalDateTime.now().plusHours(expirationHours);
        assertThat(savedToken.getExpiresAt()).isBetween(
                expectedExpiration.minusSeconds(5),
                expectedExpiration.plusSeconds(5)
        );
    }

    @Test
    void requestPasswordReset_DeletesOldTokens() {
        when(userService.findByEmail(testUser.getEmail())).thenReturn(testUser);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestPasswordReset(new ForgotPasswordRequest(testUser.getEmail()));

        verify(tokenRepository).deleteByUserId(testUser.getId());
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPassword_Success() {
        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(testToken));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.resetPassword(new ResetPasswordDto(tokenUUID, newPassword));

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userService).updatePassword(testUser.getId(), newPassword);
        verify(tokenRepository).save(testToken);
        assertThat(testToken.getUsedAt()).isNotNull();
    }

    @Test
    void resetPassword_InvalidToken() {
        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword(new ResetPasswordDto(tokenUUID, newPassword)))
                .isInstanceOf(PasswordResetTokenInvalidException.class)
                .hasMessageContaining("Invalid password reset token");

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userService, never()).updatePassword(any(), any());
    }

    @Test
    void resetPassword_ExpiredToken() {
        final PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token(tokenUUID)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .usedAt(null)
                .build();

        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(new ResetPasswordDto(tokenUUID, newPassword)))
                .isInstanceOf(PasswordResetTokenExpiredException.class)
                .hasMessageContaining("Password reset token has expired");

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userService, never()).updatePassword(any(), any());
    }

    @Test
    void resetPassword_AlreadyUsedToken() {
        final PasswordResetToken usedToken = PasswordResetToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token(tokenUUID)
                .expiresAt(LocalDateTime.now().plusHours(expirationHours))
                .usedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword(new ResetPasswordDto(tokenUUID, newPassword)))
                .isInstanceOf(PasswordResetTokenAlreadyUsedException.class)
                .hasMessageContaining("This password reset token has already been used");

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userService, never()).updatePassword(any(), any());
    }

    @Test
    void resetPassword_MarksTokenAsUsed() {
        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(testToken));
        final ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(testToken.getUsedAt()).isNull();

        passwordResetService.resetPassword(new ResetPasswordDto(tokenUUID, newPassword));

        verify(tokenRepository).save(tokenCaptor.capture());
        final PasswordResetToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUsedAt()).isNotNull();
        assertThat(savedToken.getUsedAt()).isBetween(
                LocalDateTime.now().minusSeconds(5),
                LocalDateTime.now().plusSeconds(5)
        );
    }
}
