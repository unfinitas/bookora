package fi.unfinitas.bookora.util;

import fi.unfinitas.bookora.config.BookoraProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookieUtilTest {

    @Mock
    private BookoraProperties bookoraProperties;

    @Mock
    private BookoraProperties.Cookie cookieProperties;

    @Mock
    private BookoraProperties.Jwt jwtProperties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private CookieUtil cookieUtil;

    @BeforeEach
    void setUp() {
        lenient().when(bookoraProperties.getCookie()).thenReturn(cookieProperties);
        lenient().when(bookoraProperties.getJwt()).thenReturn(jwtProperties);

        // Default cookie settings
        lenient().when(cookieProperties.isSecure()).thenReturn(false);
        lenient().when(cookieProperties.getSameSite()).thenReturn("Lax");
        lenient().when(cookieProperties.getDomain()).thenReturn("");
        lenient().when(cookieProperties.getRefreshTokenPath()).thenReturn("/api/auth");
        lenient().when(jwtProperties.getRefreshTokenExpirationSeconds()).thenReturn(604800L); // 7 days
    }

    @Test
    void shouldSetRefreshTokenCookieWithCorrectAttributes() {
        // Given
        String token = "test-refresh-token";

        // When
        cookieUtil.setRefreshTokenCookie(response, token);

        // Then
        ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(headerNameCaptor.capture(), headerValueCaptor.capture());

        assertThat(headerNameCaptor.getValue()).isEqualTo("Set-Cookie");

        String cookieHeader = headerValueCaptor.getValue();
        assertThat(cookieHeader).contains("refreshToken=test-refresh-token");
        assertThat(cookieHeader).contains("Path=/api/auth");
        assertThat(cookieHeader).contains("Max-Age=604800");
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).contains("SameSite=Lax");
    }

    @Test
    void shouldSetSecureCookieInProduction() {
        // Given
        when(cookieProperties.isSecure()).thenReturn(true);
        when(cookieProperties.getSameSite()).thenReturn("Strict");
        when(cookieProperties.getDomain()).thenReturn(".bookora.com");

        String token = "prod-refresh-token";

        // When
        cookieUtil.setRefreshTokenCookie(response, token);

        // Then
        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), headerValueCaptor.capture());

        String cookieHeader = headerValueCaptor.getValue();
        assertThat(cookieHeader).contains("Secure");
        assertThat(cookieHeader).contains("SameSite=Strict");
        assertThat(cookieHeader).contains("Domain=.bookora.com");
    }

    @Test
    void shouldGetRefreshTokenFromCookie() {
        // Given
        Cookie refreshTokenCookie = new Cookie("refreshToken", "test-token-value");
        Cookie[] cookies = {
                new Cookie("otherCookie", "other-value"),
                refreshTokenCookie,
                new Cookie("anotherCookie", "another-value")
        };
        when(request.getCookies()).thenReturn(cookies);

        // When
        Optional<String> result = cookieUtil.getRefreshTokenFromCookie(request);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("test-token-value");
    }

    @Test
    void shouldReturnEmptyWhenRefreshTokenCookieNotFound() {
        // Given
        Cookie[] cookies = {
                new Cookie("otherCookie", "other-value"),
                new Cookie("anotherCookie", "another-value")
        };
        when(request.getCookies()).thenReturn(cookies);

        // When
        Optional<String> result = cookieUtil.getRefreshTokenFromCookie(request);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoCookiesPresent() {
        // Given
        when(request.getCookies()).thenReturn(null);

        // When
        Optional<String> result = cookieUtil.getRefreshTokenFromCookie(request);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldClearRefreshTokenCookie() {
        // When
        cookieUtil.clearRefreshTokenCookie(response);

        // Then
        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), headerValueCaptor.capture());

        String cookieHeader = headerValueCaptor.getValue();
        assertThat(cookieHeader).contains("refreshToken=");
        assertThat(cookieHeader).contains("Max-Age=0"); // Delete immediately
        assertThat(cookieHeader).contains("HttpOnly");
        assertThat(cookieHeader).contains("Path=/api/auth");
    }

    @Test
    void shouldHandleEmptyDomainCorrectly() {
        // Given
        when(cookieProperties.getDomain()).thenReturn("");
        String token = "test-token";

        // When
        cookieUtil.setRefreshTokenCookie(response, token);

        // Then
        ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), headerValueCaptor.capture());

        String cookieHeader = headerValueCaptor.getValue();
        // Domain should not be set when empty
        assertThat(cookieHeader).doesNotContain("Domain=");
    }
}