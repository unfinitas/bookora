package fi.unfinitas.bookora.service.impl;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import fi.unfinitas.bookora.exception.EmailNotVerifiedException;
import fi.unfinitas.bookora.exception.InvalidCredentialsException;
import fi.unfinitas.bookora.mapper.UserMapper;
import fi.unfinitas.bookora.security.CustomUserDetails;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.AuthenticationService;
import fi.unfinitas.bookora.service.EmailVerificationService;
import fi.unfinitas.bookora.service.UserService;
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
     *
     * @param request the login request
     * @return login data with tokens
     */
    @Override
    public LoginResponse login(final LoginRequest request) {
        log.debug("Attempting to authenticate user");

        try {
            final UserDetails userDetails = authenticateUser(request);
            final User user = ((CustomUserDetails) userDetails).getUser();

            log.debug("User authenticated successfully with ID: {}", user.getId());

            return buildLoginResponse(user, userDetails);

        } catch (final InternalAuthenticationServiceException e) {
            // Spring Security wraps exceptions from UserDetailsService
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
     * Refresh access token using refresh token
     *
     * @param refreshToken the refresh token
     * @return new login data with tokens
     */
    @Override
    public LoginResponse refreshToken(final String refreshToken) {
        log.debug("Attempting to refresh token");

        try {
            final String username = jwtUtil.extractUsername(refreshToken);
            final UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            validateRefreshToken(refreshToken, userDetails, username);

            final User user = ((CustomUserDetails) userDetails).getUser();

            log.debug("Token refreshed successfully for user ID: {}", user.getId());

            return buildLoginResponse(user, userDetails);
        } catch (final EmailNotVerifiedException e) {
            log.warn("Email not verified during token refresh");
            throw e;
        }
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

    private void validateRefreshToken(final String refreshToken, final UserDetails userDetails, final String username) {
        if (!jwtUtil.validateToken(refreshToken, userDetails)) {
            log.warn("Invalid refresh token");
            throw new InvalidCredentialsException("Invalid refresh token");
        }
    }

    private LoginResponse buildLoginResponse(final User user, final UserDetails userDetails) {
        final String accessToken = jwtUtil.generateAccessToken(userDetails);
        final String refreshToken = jwtUtil.generateRefreshToken(userDetails);
        final Long expiresIn = jwtUtil.getAccessTokenExpiration();

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn
        );
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
