package fi.unfinitas.bookora.config.security;

import fi.unfinitas.bookora.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that intercepts requests and validates JWT tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            final String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && isAuthenticationRequired()) {
                authenticateUser(jwt, request);
            }
        } catch (final JwtException e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        } catch (final Exception e) {
            log.error("Unexpected error during JWT authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Extract JWT token from Authorization header.
     */
    private String extractJwtFromRequest(final HttpServletRequest request) {
        final String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Check if authentication is required (not already authenticated).
     */
    private boolean isAuthenticationRequired() {
        return SecurityContextHolder.getContext().getAuthentication() == null;
    }

    /**
     * Authenticate user with JWT token.
     */
    private void authenticateUser(final String jwt, final HttpServletRequest request) {
        final String userEmail = jwtUtil.extractUsername(jwt);

        if (!StringUtils.hasText(userEmail)) {
            log.warn("JWT token does not contain username");
            return;
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (jwtUtil.validateToken(jwt, userDetails)) {
            setAuthentication(userDetails, request);
            log.debug("Successfully authenticated user: {}", userEmail);
        } else {
            log.warn("JWT token validation failed for user: {}", userEmail);
        }
    }

    /**
     * Set authentication in SecurityContext.
     */
    private void setAuthentication(final UserDetails userDetails, final HttpServletRequest request) {
        final UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
