package fi.unfinitas.bookora.service.impl;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.domain.model.RefreshToken;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import fi.unfinitas.bookora.exception.EmailNotVerifiedException;
import fi.unfinitas.bookora.exception.InvalidCredentialsException;
import fi.unfinitas.bookora.mapper.UserMapper;
import fi.unfinitas.bookora.security.CustomUserDetails;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.AuthenticationService;
import fi.unfinitas.bookora.service.EmailVerificationService;
import fi.unfinitas.bookora.service.RefreshTokenService;
import fi.unfinitas.bookora.service.UserService;
import fi.unfinitas.bookora.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of AuthenticationService.
 * Handles user authentication operations including login, registration, and token refresh.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;
    private final EmailVerificationService emailVerificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final BookoraProperties bookoraProperties;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;

    /**
     * Register a new user
     *
     * @param request the registration request
     * @return user public information
     */
    @Override
    public UserPublicInfo register(final RegisterRequest request) {
        log.debug("Attempting to register new user");

        final User savedUser = userService.createUser(request);

        final EmailVerificationToken token = emailVerificationService.generateVerificationToken(savedUser);

        userService.updateLastVerificationEmailSentAt(savedUser.getId());

        publishVerificationEmail(savedUser, token);

        log.debug("User registered successfully with ID: {}. Verification email sent.", savedUser.getId());

        return userMapper.toUserResponse(savedUser);
    }

    /**
     * Authenticate a user and generate tokens.
     * Refresh token is set as HttpOnly cookie directly.
     *
     * @param request the login request
     * @param response HTTP response for setting cookies
     * @return login data with access token only
     */
    @Override
    public LoginResponse login(final LoginRequest request, final HttpServletResponse response) {
        log.debug("Attempting to authenticate user");

        try {
            final UserDetails userDetails = authenticateUser(request);
            final User user = ((CustomUserDetails) userDetails).getUser();

            log.debug("User authenticated successfully with ID: {}", user.getId());

            // Generate access token
            final String accessToken = jwtUtil.generateAccessToken(userDetails);
            final Long expiresIn = jwtUtil.getAccessTokenExpiration();

            // Create and set refresh token as HttpOnly cookie
            final RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), UUID.randomUUID());
            cookieUtil.setRefreshTokenCookie(response, refreshToken.getRawToken());

            return new LoginResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getRole().name(),
                    accessToken,
                    "Bearer",
                    expiresIn
            );

        } catch (final InternalAuthenticationServiceException e) {
            if (e.getCause() instanceof EmailNotVerifiedException) {
                log.warn("Email not verified for user: {}", request.username());
                throw (EmailNotVerifiedException) e.getCause();
            }
            throw e;
        } catch (final BadCredentialsException e) {
            log.warn("Authentication failed");
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    /**
     * Refresh access token using refresh token from cookie.
     * New refresh token is set as HttpOnly cookie directly.
     *
     * @param request HTTP request to get refresh token from cookie
     * @param response HTTP response for setting cookies
     * @return new login data with access token only
     */
    @Override
    public LoginResponse refreshToken(final HttpServletRequest request, final HttpServletResponse response) {
        log.debug("Attempting to refresh token");

        // Get refresh token from cookie
        final String rawRefreshToken = cookieUtil.getRefreshTokenFromCookie(request)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token not found"));

        // Validate and rotate refresh token
        final RefreshToken newRefreshToken = refreshTokenService.validateAndRotateToken(rawRefreshToken);

        // Load user
        final User user = userService.findById(newRefreshToken.getUserId());

        // Create UserDetails directly from User without additional DB query
        final UserDetails userDetails = new CustomUserDetails(user);

        log.debug("Token refreshed successfully for user ID: {}", user.getId());

        // Generate new access token
        final String accessToken = jwtUtil.generateAccessToken(userDetails);
        final Long expiresIn = jwtUtil.getAccessTokenExpiration();

        // Set new refresh token as HttpOnly cookie
        cookieUtil.setRefreshTokenCookie(response, newRefreshToken.getRawToken());

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                accessToken,
                "Bearer",
                expiresIn
        );
    }

    /**
     * Logout user by revoking refresh token and clearing cookie.
     *
     * @param request HTTP request to get refresh token from cookie
     * @param response HTTP response for clearing cookies
     */
    @Override
    public void logout(final HttpServletRequest request, final HttpServletResponse response) {
        log.debug("Attempting to logout user");

        // Get refresh token from cookie if present and revoke it
        cookieUtil.getRefreshTokenFromCookie(request).ifPresent(token -> {
            try {
                refreshTokenService.revokeToken(token);
                log.debug("Refresh token revoked during logout");
            } catch (Exception e) {
                log.warn("Failed to revoke refresh token during logout", e);
            }
        });

        // Clear the refresh token cookie
        cookieUtil.clearRefreshTokenCookie(response);

        log.debug("Logout completed");
    }

    private UserDetails authenticateUser(final LoginRequest request) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );
        return (UserDetails) authentication.getPrincipal();
    }

    private void publishVerificationEmail(final User user, final EmailVerificationToken token) {
        try {
            final int expirationDays = bookoraProperties.getVerification().getToken().getExpirationDays();

            final Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("firstName", user.getFirstName());
            templateVariables.put("verificationLink", bookoraProperties.getFrontendUrl() + "/auth/verify/" + token.getToken());
            templateVariables.put("expirationDays", expirationDays);
            templateVariables.put("frontendUrl", bookoraProperties.getFrontendUrl());

            final SendMailEvent event = new SendMailEvent(
                    user.getEmail(),
                    "Verify Your Email - Bookora",
                    "email/email-verification",
                    templateVariables
            );

            eventPublisher.publishEvent(event);
            log.debug("Published SendMailEvent for email verification to user ID: {}", user.getId());
        } catch (final Exception e) {
            log.error("Failed to publish SendMailEvent for user ID: {}", user.getId(), e);
        }
    }
}
