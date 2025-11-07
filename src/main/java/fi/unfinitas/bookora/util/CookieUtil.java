package fi.unfinitas.bookora.util;

import fi.unfinitas.bookora.config.BookoraProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private final BookoraProperties bookoraProperties;

    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        long maxAge = bookoraProperties.getJwt().getRefreshTokenExpirationSeconds();

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(bookoraProperties.getCookie().isSecure())
                .path(bookoraProperties.getCookie().getRefreshTokenPath())
                .maxAge(maxAge)
                .sameSite(bookoraProperties.getCookie().getSameSite())
                .domain(bookoraProperties.getCookie().getDomain().isEmpty() ? null : bookoraProperties.getCookie().getDomain())
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("Set refresh token cookie with maxAge: {} seconds", maxAge);
    }

    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(bookoraProperties.getCookie().isSecure())
                .path(bookoraProperties.getCookie().getRefreshTokenPath())
                .maxAge(0)  // Delete cookie immediately
                .sameSite(bookoraProperties.getCookie().getSameSite())
                .domain(bookoraProperties.getCookie().getDomain().isEmpty() ? null : bookoraProperties.getCookie().getDomain())
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("Cleared refresh token cookie");
    }
}