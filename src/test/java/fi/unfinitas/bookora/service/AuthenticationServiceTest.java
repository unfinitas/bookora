package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.enums.UserRole;
import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import fi.unfinitas.bookora.domain.model.RefreshToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import fi.unfinitas.bookora.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import fi.unfinitas.bookora.exception.EmailAlreadyExistsException;
import fi.unfinitas.bookora.exception.InvalidCredentialsException;
import fi.unfinitas.bookora.exception.UserNotFoundException;
import fi.unfinitas.bookora.exception.UsernameAlreadyExistsException;
import fi.unfinitas.bookora.mapper.UserMapper;
import fi.unfinitas.bookora.security.CustomUserDetails;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.impl.AuthenticationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
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

    @Mock
    private BookoraProperties.Jwt jwtConfig;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

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
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userService.createUser(registerRequest))
                .thenThrow(new EmailAlreadyExistsException("Email is already registered: " + registerRequest.getEmail()));

        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("Email is already registered");

        verify(userService).createUser(registerRequest);
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        when(userService.createUser(registerRequest))
                .thenThrow(new UsernameAlreadyExistsException("Username is already taken: " + registerRequest.getUsername()));

        assertThatThrownBy(() -> authenticationService.register(registerRequest))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("Username is already taken");

        verify(userService).createUser(registerRequest);
    }


    @Test
    void shouldSuccessfullyLoginUser() {
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(86400000L);

        RefreshToken refreshToken = RefreshToken.builder()
                .id(1L)
                .userId(testUser.getId())
                .tokenHash("hash123")
                .tokenFamily(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshToken.setRawToken("refresh-token");
        when(refreshTokenService.createRefreshToken(any(UUID.class), any(UUID.class))).thenReturn(refreshToken);

        final LoginResponse result = authenticationService.login(loginRequest, response);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(86400000L);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateAccessToken(userDetails);
        verify(refreshTokenService).createRefreshToken(any(UUID.class), any(UUID.class));
        verify(cookieUtil).setRefreshTokenCookie(response, "refresh-token");
    }

    @Test
    void shouldThrowExceptionWhenCredentialsAreInvalid() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authenticationService.login(loginRequest, response))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil, never()).generateAccessToken(any());
    }

    @Test
    void shouldSuccessfullyRefreshToken() {
        final String rawToken = "valid-refresh-token";
        when(cookieUtil.getRefreshTokenFromCookie(request)).thenReturn(Optional.of(rawToken));

        RefreshToken newRefreshToken = RefreshToken.builder()
                .id(2L)
                .userId(testUser.getId())
                .tokenHash("new-hash")
                .tokenFamily(UUID.randomUUID())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        newRefreshToken.setRawToken("new-refresh-token");
        when(refreshTokenService.validateAndRotateToken(rawToken)).thenReturn(newRefreshToken);
        when(userService.findById(testUser.getId())).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(any(UserDetails.class))).thenReturn("new-access-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(86400000L);

        final LoginResponse result = authenticationService.refreshToken(request, response);

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.tokenType()).isEqualTo("Bearer");

        verify(cookieUtil).getRefreshTokenFromCookie(request);
        verify(refreshTokenService).validateAndRotateToken(rawToken);
        verify(userService).findById(testUser.getId());
        verify(jwtUtil).generateAccessToken(any(UserDetails.class));
        verify(cookieUtil).setRefreshTokenCookie(response, "new-refresh-token");
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenNotFoundInCookie() {
        when(cookieUtil.getRefreshTokenFromCookie(request)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.refreshToken(request, response))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Refresh token not found");

        verify(cookieUtil).getRefreshTokenFromCookie(request);
        verify(refreshTokenService, never()).validateAndRotateToken(any());
    }

    @Test
    void shouldSuccessfullyLogoutUser() {
        final String rawToken = "refresh-token";
        when(cookieUtil.getRefreshTokenFromCookie(request)).thenReturn(Optional.of(rawToken));

        authenticationService.logout(request, response);

        verify(cookieUtil).getRefreshTokenFromCookie(request);
        verify(refreshTokenService).revokeToken(rawToken);
        verify(cookieUtil).clearRefreshTokenCookie(response);
    }

    @Test
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
