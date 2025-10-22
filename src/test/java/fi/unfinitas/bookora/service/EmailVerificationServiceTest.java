package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.exception.RateLimitExceededException;
import fi.unfinitas.bookora.exception.UserNotFoundException;
import fi.unfinitas.bookora.exception.VerificationTokenExpiredException;
import fi.unfinitas.bookora.exception.VerificationTokenInvalidException;
import fi.unfinitas.bookora.repository.EmailVerificationTokenRepository;
import fi.unfinitas.bookora.repository.UserRepository;
import fi.unfinitas.bookora.service.impl.EmailVerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BookoraProperties bookoraProperties;

    @Mock
    private BookoraProperties.Verification verification;

    @Mock
    private BookoraProperties.Verification.Token tokenConfig;

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    private User testUser;
    private EmailVerificationToken testToken;
    private UUID tokenUUID;
    private final int expirationDays = 7;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("encodedPassword")
                .isGuest(false)
                .isEmailVerified(false)
                .lastVerificationEmailSentAt(null)
                .build();

        tokenUUID = UUID.randomUUID();

        testToken = EmailVerificationToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token(tokenUUID)
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .usedAt(null)
                .build();

        // Setup nested property mocking with lenient() since not all tests use all mocks
        lenient().when(bookoraProperties.getVerification()).thenReturn(verification);
        lenient().when(verification.getToken()).thenReturn(tokenConfig);
        lenient().when(tokenConfig.getExpirationDays()).thenReturn(expirationDays);
        lenient().when(bookoraProperties.getFrontendUrl()).thenReturn("http://localhost:3000");
    }

    @Test
    @DisplayName("Should generate verification token successfully")
    void shouldGenerateVerificationTokenSuccessfully() {
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final EmailVerificationToken result = emailVerificationService.generateVerificationToken(testUser);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(testUser.getId());
        assertThat(result.getToken()).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(result.getUsedAt()).isNull();

        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("Should calculate correct expiration time when generating token")
    void shouldCalculateCorrectExpirationTime() {
        final ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.generateVerificationToken(testUser);

        verify(tokenRepository).save(tokenCaptor.capture());
        final EmailVerificationToken savedToken = tokenCaptor.getValue();

        final LocalDateTime expectedExpiration = LocalDateTime.now().plusDays(expirationDays);
        assertThat(savedToken.getExpiresAt()).isBetween(
                expectedExpiration.minusSeconds(5),
                expectedExpiration.plusSeconds(5)
        );
    }

    @Test
    @DisplayName("Should verify email with valid token")
    void shouldVerifyEmailWithValidToken() {
        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(testToken));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.verifyEmail(tokenUUID);

        assertThat(testUser.getIsEmailVerified()).isTrue();
        assertThat(testToken.getUsedAt()).isNotNull();

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).save(testUser);
        verify(tokenRepository).save(testToken);
    }

    @Test
    @DisplayName("Should throw exception when token not found")
    void shouldThrowExceptionWhenTokenNotFound() {
        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(tokenUUID))
                .isInstanceOf(VerificationTokenInvalidException.class)
                .hasMessageContaining("Invalid verification token");

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when token is expired")
    void shouldThrowExceptionWhenTokenIsExpired() {
        final EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token(tokenUUID)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .usedAt(null)
                .build();

        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(tokenUUID))
                .isInstanceOf(VerificationTokenExpiredException.class)
                .hasMessageContaining("Verification token has expired");

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when token is already used")
    void shouldThrowExceptionWhenTokenIsAlreadyUsed() {
        final EmailVerificationToken usedToken = EmailVerificationToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .token(tokenUUID)
                .expiresAt(LocalDateTime.now().plusDays(expirationDays))
                .usedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(tokenUUID))
                .isInstanceOf(VerificationTokenInvalidException.class)
                .hasMessageContaining("Verification token has already been used");

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found for token")
    void shouldThrowExceptionWhenUserNotFoundForToken() {
        when(tokenRepository.findByToken(tokenUUID)).thenReturn(Optional.of(testToken));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(tokenUUID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(tokenRepository).findByToken(tokenUUID);
        verify(userRepository).findById(testUser.getId());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should resend verification email successfully")
    void shouldResendVerificationEmailSuccessfully() {
        testUser.setLastVerificationEmailSentAt(LocalDateTime.now().minusHours(2));

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.resendVerificationEmail(testUser.getEmail());

        verify(userRepository).findByEmail(testUser.getEmail());
        verify(tokenRepository).deleteByUserId(testUser.getId());
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(userRepository).save(testUser);
        verify(eventPublisher).publishEvent(any(fi.unfinitas.bookora.domain.event.SendMailEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when resend rate limit exceeded")
    void shouldThrowExceptionWhenResendRateLimitExceeded() {
        testUser.setLastVerificationEmailSentAt(LocalDateTime.now().minusMinutes(30));

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> emailVerificationService.resendVerificationEmail(testUser.getEmail()))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Please wait at least 1 hour");

        verify(userRepository).findByEmail(testUser.getEmail());
        verify(tokenRepository, never()).deleteByUserId(any());
        verify(tokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should delete old tokens when resending verification email")
    void shouldDeleteOldTokensWhenResendingVerificationEmail() {
        testUser.setLastVerificationEmailSentAt(LocalDateTime.now().minusHours(2));

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.resendVerificationEmail(testUser.getEmail());

        verify(tokenRepository).deleteByUserId(testUser.getId());
        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for resend")
    void shouldThrowExceptionWhenUserNotFoundForResend() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.resendVerificationEmail(testUser.getEmail()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with email");

        verify(userRepository).findByEmail(testUser.getEmail());
        verify(tokenRepository, never()).deleteByUserId(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should allow resend when lastVerificationEmailSentAt is null")
    void shouldAllowResendWhenLastVerificationEmailSentAtIsNull() {
        testUser.setLastVerificationEmailSentAt(null);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.resendVerificationEmail(testUser.getEmail());

        verify(userRepository).findByEmail(testUser.getEmail());
        verify(tokenRepository).deleteByUserId(testUser.getId());
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(eventPublisher).publishEvent(any(fi.unfinitas.bookora.domain.event.SendMailEvent.class));
    }

    @Test
    @DisplayName("Should update lastVerificationEmailSentAt when resending")
    void shouldUpdateLastVerificationEmailSentAtWhenResending() {
        testUser.setLastVerificationEmailSentAt(LocalDateTime.now().minusHours(2));
        final LocalDateTime originalSentAt = testUser.getLastVerificationEmailSentAt();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.resendVerificationEmail(testUser.getEmail());

        assertThat(testUser.getLastVerificationEmailSentAt()).isAfter(originalSentAt);
        verify(userRepository).save(testUser);
    }
}
