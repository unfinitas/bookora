package fi.unfinitas.bookora.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;


class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();

        ReflectionTestUtils.setField(jwtUtil, "secret", "ThisIsAVerySecureSecretKeyForJWTTesting123456789");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L); // 7 days

        jwtUtil.init();

        userDetails = User.builder()
                .username("testuser")
                .password("Password123!")
                .authorities(Set.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Test
    @DisplayName("Should successfully initialize with valid secret key")
    void shouldInitializeWithValidSecretKey() {
        final JwtUtil newJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(newJwtUtil, "secret", "ThisIsAVerySecureSecretKeyForJWTTesting123456789");
        ReflectionTestUtils.setField(newJwtUtil, "expiration", 86400000L);
        ReflectionTestUtils.setField(newJwtUtil, "refreshExpiration", 604800000L);

        assertThatCode(newJwtUtil::init).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when secret key is too short")
    void shouldThrowExceptionWhenSecretKeyTooShort() {
        final JwtUtil newJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(newJwtUtil, "secret", "short_key");
        ReflectionTestUtils.setField(newJwtUtil, "expiration", 86400000L);
        ReflectionTestUtils.setField(newJwtUtil, "refreshExpiration", 604800000L);

        assertThatThrownBy(newJwtUtil::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key too short");
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        final String token = jwtUtil.generateRefreshToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsernameFromToken() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        final String username = jwtUtil.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void shouldExtractExpirationFromToken() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        final Date expiration = jwtUtil.extractExpiration(token);

        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateTokenSuccessfully() {
        final String token = jwtUtil.generateAccessToken(userDetails);

        final Boolean isValid = jwtUtil.validateToken(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should fail validation for different user")
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
    @DisplayName("Should fail validation for expired token")
    void shouldFailValidationForExpiredToken() {
        final JwtUtil shortExpirationJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "secret", "ThisIsAVerySecureSecretKeyForJWTTesting123456789");
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "expiration", -1000L); // Already expired
        ReflectionTestUtils.setField(shortExpirationJwtUtil, "refreshExpiration", 604800000L);
        shortExpirationJwtUtil.init();

        final String expiredToken = shortExpirationJwtUtil.generateAccessToken(userDetails);

        assertThatThrownBy(() -> shortExpirationJwtUtil.validateToken(expiredToken, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should throw exception for malformed token")
    void shouldThrowExceptionForMalformedToken() {
        final String malformedToken = "not.a.valid.jwt.token";

        assertThatThrownBy(() -> jwtUtil.extractUsername(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Should return correct access token expiration")
    void shouldReturnCorrectAccessTokenExpiration() {
        final Long expiration = jwtUtil.getAccessTokenExpiration();

        assertThat(expiration).isEqualTo(86400000L); // 24 hours in milliseconds
    }

    @Test
    @DisplayName("Access token and refresh token should have different expiration times")
    void accessTokenAndRefreshTokenShouldHaveDifferentExpirationTimes() {
        final String accessToken = jwtUtil.generateAccessToken(userDetails);
        final String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        final Date accessExpiration = jwtUtil.extractExpiration(accessToken);
        final Date refreshExpiration = jwtUtil.extractExpiration(refreshToken);

        assertThat(refreshExpiration).isAfter(accessExpiration);
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void shouldGenerateDifferentTokensForSameUserAtDifferentTimes() throws InterruptedException {
        final String token1 = jwtUtil.generateAccessToken(userDetails);

        Thread.sleep(2000);

        final String token2 = jwtUtil.generateAccessToken(userDetails);

        assertThat(token1).isNotEqualTo(token2);
    }
}
