package fi.unfinitas.bookora.security;

import fi.unfinitas.bookora.config.BookoraProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private BookoraProperties bookoraProperties;

    @Mock
    private BookoraProperties.Jwt jwtProperties;

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        lenient().when(bookoraProperties.getJwt()).thenReturn(jwtProperties);
        lenient().when(jwtProperties.getAccessTokenExpirationSeconds()).thenReturn(86400L); // 24 hours in seconds
        lenient().when(jwtProperties.getRefreshTokenExpirationSeconds()).thenReturn(604800L); // 7 days in seconds

        jwtUtil = new JwtUtil(bookoraProperties);
        // Use reflection to set the secret since it's injected via @Value
        ReflectionTestUtils.setField(jwtUtil, "secret", "ThisIsAVerySecureSecretKeyForJWTTesting123456789");
        jwtUtil.init();

        userDetails = User.builder()
                .username("testuser")
                .password("Password123!")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    void shouldInitializeWithValidSecretKey() {
        final BookoraProperties props = mock(BookoraProperties.class);
        final BookoraProperties.Jwt jwt = mock(BookoraProperties.Jwt.class);
        lenient().when(props.getJwt()).thenReturn(jwt);
        lenient().when(jwt.getAccessTokenExpirationSeconds()).thenReturn(86400L);

        final JwtUtil newJwtUtil = new JwtUtil(props);
        ReflectionTestUtils.setField(newJwtUtil, "secret", "ThisIsAVerySecureSecretKeyForJWTTesting123456789");
        assertThatCode(newJwtUtil::init).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowExceptionWhenSecretKeyTooShort() {
        final BookoraProperties props = mock(BookoraProperties.class);
        final BookoraProperties.Jwt jwt = mock(BookoraProperties.Jwt.class);
        lenient().when(props.getJwt()).thenReturn(jwt);
        lenient().when(jwt.getAccessTokenExpirationSeconds()).thenReturn(86400L);

        final JwtUtil newJwtUtil = new JwtUtil(props);
        ReflectionTestUtils.setField(newJwtUtil, "secret", "short_key");
        assertThatThrownBy(newJwtUtil::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key too short");
    }

    @Test
    void shouldGenerateValidAccessToken() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void shouldExtractUsernameFromToken() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        final String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void shouldExtractExpirationFromToken() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        final Date expiration = jwtUtil.extractExpiration(token);

        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        final Boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    void shouldFailValidationForDifferentUser() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        final UserDetails differentUser = User.builder()
                .username("differentuser")
                .password("Password123!")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        final Boolean isValid = jwtUtil.validateToken(token, differentUser);

        assertThat(isValid).isFalse();
    }

    @Test
    void shouldFailValidationForExpiredToken() {
        final BookoraProperties props = mock(BookoraProperties.class);
        final BookoraProperties.Jwt jwt = mock(BookoraProperties.Jwt.class);
        lenient().when(props.getJwt()).thenReturn(jwt);
        lenient().when(jwt.getAccessTokenExpirationSeconds()).thenReturn(-1L); // Already expired

        final JwtUtil shortExpirationJwtUtil = new JwtUtil(props);
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", "ThisIsAVerySecureSecretKeyForJWTTesting123456789");
        shortExpirationJwtUtil.init();

        final String expiredToken = shortExpirationJwtUtil.generateAccessToken(userDetails);

        assertThatThrownBy(() -> shortExpirationJwtUtil.validateToken(expiredToken, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldThrowExceptionForMalformedToken() {
        final String malformedToken = "not.a.valid.jwt.token";

        assertThatThrownBy(() -> jwtUtil.extractUsername(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void shouldReturnCorrectAccessTokenExpiration() {
        final Long expiration = jwtUtil.getAccessTokenExpiration();

        assertThat(expiration).isEqualTo(86400000L); // 24 hours in milliseconds
    }

    @Test
    void shouldGenerateDifferentTokensForSameUserAtDifferentTimes() throws InterruptedException {
        final String token1 = jwtUtil.generateAccessToken(userDetails);

        Thread.sleep(2000);

        final String token2 = jwtUtil.generateAccessToken(userDetails);

        assertThat(token1).isNotEqualTo(token2);
    }
}
