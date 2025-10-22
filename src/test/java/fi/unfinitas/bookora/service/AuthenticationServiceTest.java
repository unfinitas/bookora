package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.enums.UserRole;
import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import fi.unfinitas.bookora.exception.EmailAlreadyExistsException;
import fi.unfinitas.bookora.exception.InvalidCredentialsException;
import fi.unfinitas.bookora.exception.UserNotFoundException;
import fi.unfinitas.bookora.exception.UsernameAlreadyExistsException;
import fi.unfinitas.bookora.mapper.UserMapper;
import fi.unfinitas.bookora.security.CustomUserDetails;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.springframework.security.core.userdetails.User.builder;

/**
 * Unit tests for AuthenticationServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BookoraProperties bookoraProperties;

    @Mock
    private BookoraProperties.Verification verification;

    @Mock
    private BookoraProperties.Verification.Token tokenConfig;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("Password123!")
                .build();

        loginRequest = new LoginRequest("testuser", "Password123!");

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(UserRole.USER)
                .isGuest(false)
                .isEmailVerified(false)
                .build();

        userDetails = new CustomUserDetails(testUser);

        // Setup nested property mocking for BookoraProperties with lenient() since not all tests use all mocks
        lenient().when(bookoraProperties.getVerification()).thenReturn(verification);
        lenient().when(verification.getToken()).thenReturn(tokenConfig);
        lenient().when(tokenConfig.getExpirationDays()).thenReturn(7);
        lenient().when(bookoraProperties.getFrontendUrl()).thenReturn("http://localhost:3000");
    }

    @Test
    @DisplayName("Should successfully register new user")
    void shouldSuccessfullyRegisterNewUser() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(testUser.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(userService.createUser(registerRequest)).thenReturn(testUser);
        when(emailVerificationService.generateVerificationToken(testUser)).thenReturn(token);
        doNothing().when(userService).updateLastVerificationEmailSentAt(testUser.getId());
        doNothing().when(eventPublisher).publishEvent(any());

        final UserPublicInfo userPublicInfo = UserPublicInfo.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .role(testUser.getRole().name())
                .build();
        when(userMapper.toUserResponse(testUser)).thenReturn(userPublicInfo);

        final UserPublicInfo result = authenticationService.register(registerRequest);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRole()).isEqualTo("USER");

        verify(userService).createUser(registerRequest);
        verify(emailVerificationService).generateVerificationToken(testUser);
        verify(userService).updateLastVerificationEmailSentAt(testUser.getId());
        verify(eventPublisher).publishEvent(any(fi.unfinitas.bookora.domain.event.SendMailEvent.class));
        verify(userMapper).toUserResponse(testUser);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userService.createUser(registerRequest))
                .thenThrow(new EmailAlreadyExistsException("Email is already registered: " + registerRequest.getEmail()));

        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email is already registered");

        verify(userService).createUser(registerRequest);
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        when(userService.createUser(registerRequest))
                .thenThrow(new UsernameAlreadyExistsException("Username is already taken: " + registerRequest.getUsername()));

        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("Username is already taken");

        verify(userService).createUser(registerRequest);
    }


    @Test
    @DisplayName("Should successfully login user")
    void shouldSuccessfullyLoginUser() {
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(86400000L);

        final LoginResponse result = authenticationService.login(loginRequest);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(86400000L);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateAccessToken(userDetails);
        verify(jwtUtil).generateRefreshToken(userDetails);
    }

    @Test
    @DisplayName("Should throw exception when credentials are invalid")
    void shouldThrowExceptionWhenCredentialsAreInvalid() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should successfully refresh token")
    void shouldSuccessfullyRefreshToken() {
        final String refreshToken = "valid-refresh-token";
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(refreshToken, userDetails)).thenReturn(true);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("new-refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(86400000L);

        final LoginResponse result = authenticationService.refreshToken(refreshToken);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");

        verify(jwtUtil).extractUsername(refreshToken);
        verify(jwtUtil).validateToken(refreshToken, userDetails);
        verify(jwtUtil).generateAccessToken(userDetails);
        verify(jwtUtil).generateRefreshToken(userDetails);
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void shouldThrowExceptionWhenRefreshTokenIsInvalid() {
        final String refreshToken = "invalid-refresh-token";
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtil.validateToken(refreshToken, userDetails)).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.refreshToken(refreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(jwtUtil).validateToken(refreshToken, userDetails);
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should throw exception when user not found during token refresh")
    void shouldThrowExceptionWhenUserNotFoundDuringTokenRefresh() {
        final String refreshToken = "valid-refresh-token";
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("nonexistent");
        when(userDetailsService.loadUserByUsername("nonexistent"))
                .thenThrow(new UserNotFoundException("User not found with username: nonexistent"));

        assertThatThrownBy(() -> authenticationService.refreshToken(refreshToken))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userDetailsService).loadUserByUsername("nonexistent");
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should generate verification token when registering")
    void shouldGenerateVerificationTokenWhenRegistering() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(testUser.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(userService.createUser(registerRequest)).thenReturn(testUser);
        when(emailVerificationService.generateVerificationToken(testUser)).thenReturn(token);
        doNothing().when(userService).updateLastVerificationEmailSentAt(testUser.getId());
        doNothing().when(eventPublisher).publishEvent(any());

        final UserPublicInfo userPublicInfo = UserPublicInfo.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .role(testUser.getRole().name())
                .build();
        when(userMapper.toUserResponse(testUser)).thenReturn(userPublicInfo);

        authenticationService.register(registerRequest);

        verify(emailVerificationService).generateVerificationToken(testUser);
    }

    @Test
    @DisplayName("Should send verification email when registering")
    void shouldSendVerificationEmailWhenRegistering() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(testUser.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(userService.createUser(registerRequest)).thenReturn(testUser);
        when(emailVerificationService.generateVerificationToken(testUser)).thenReturn(token);
        doNothing().when(userService).updateLastVerificationEmailSentAt(testUser.getId());
        doNothing().when(eventPublisher).publishEvent(any());

        final UserPublicInfo userPublicInfo = UserPublicInfo.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .role(testUser.getRole().name())
                .build();
        when(userMapper.toUserResponse(testUser)).thenReturn(userPublicInfo);

        authenticationService.register(registerRequest);

        verify(eventPublisher).publishEvent(any(fi.unfinitas.bookora.domain.event.SendMailEvent.class));
    }

    @Test
    @DisplayName("Should update lastVerificationEmailSentAt when registering")
    void shouldUpdateLastVerificationEmailSentAtWhenRegistering() {
        final EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(testUser.getId())
                .token(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(userService.createUser(registerRequest)).thenReturn(testUser);
        when(emailVerificationService.generateVerificationToken(testUser)).thenReturn(token);
        doNothing().when(userService).updateLastVerificationEmailSentAt(testUser.getId());
        doNothing().when(eventPublisher).publishEvent(any());

        final UserPublicInfo userPublicInfo = UserPublicInfo.builder()
                .id(testUser.getId())
                .username(testUser.getUsername())
                .role(testUser.getRole().name())
                .build();
        when(userMapper.toUserResponse(testUser)).thenReturn(userPublicInfo);

        authenticationService.register(registerRequest);

        verify(userService).updateLastVerificationEmailSentAt(testUser.getId());
    }
}
