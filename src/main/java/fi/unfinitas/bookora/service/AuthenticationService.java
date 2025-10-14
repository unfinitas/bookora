package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;

/**
 * Service interface for handling user authentication operations.
 * Responsible for login, registration, and token refresh operations.
 */
public interface AuthenticationService {

    /**
     * Register a new user
     *
     * @param request the registration request
     * @return user public information
     */
    UserPublicInfo register(RegisterRequest request);

    /**
     * Authenticate a user and generate tokens.
     *
     * @param request the login request
     * @return login data with tokens
     */
    LoginResponse login(LoginRequest request);

    /**
     * Refresh access token using refresh token
     *
     * @param refreshToken the refresh token
     * @return new login data with tokens
     */
    LoginResponse refreshToken(String refreshToken);
}
