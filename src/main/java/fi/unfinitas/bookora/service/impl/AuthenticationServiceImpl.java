package fi.unfinitas.bookora.service.impl;

import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import fi.unfinitas.bookora.exception.InvalidCredentialsException;
import fi.unfinitas.bookora.mapper.UserMapper;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.AuthenticationService;
import fi.unfinitas.bookora.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

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

        log.debug("User registered successfully with ID: {}", savedUser.getId());

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
            final User user = userService.findByUsername(userDetails.getUsername());

            log.debug("User authenticated successfully with ID: {}", user.getId());

            return buildLoginResponse(user, userDetails);

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

        final String username = jwtUtil.extractUsername(refreshToken);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        validateRefreshToken(refreshToken, userDetails, username);

        final User user = userService.findByUsername(username);

        log.debug("Token refreshed successfully for user ID: {}", user.getId());

        return buildLoginResponse(user, userDetails);
    }

    /**
     * Authenticate user with credentials.
     */
    private UserDetails authenticateUser(final LoginRequest request) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );
        return (UserDetails) authentication.getPrincipal();
    }

    /**
     * Validate refresh token.
     */
    private void validateRefreshToken(final String refreshToken, final UserDetails userDetails, final String username) {
        if (!jwtUtil.validateToken(refreshToken, userDetails)) {
            log.warn("Invalid refresh token");
            throw new InvalidCredentialsException("Invalid refresh token");
        }
    }

    /**
     * Build login response with tokens.
     */
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
}
