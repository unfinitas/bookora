package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
     * Refresh token is set as HttpOnly cookie directly.
     *
     * @param request the login request
     * @param response HTTP response for setting cookies
     * @return login data with access token only
     */
    LoginResponse login(LoginRequest request, HttpServletResponse response);

    /**
     * Refresh access token using refresh token from cookie.
     * New refresh token is set as HttpOnly cookie directly.
     *
     * @param request HTTP request to get refresh token from cookie
     * @param response HTTP response for setting cookies
     * @return new login data with access token only
     */
    LoginResponse refreshToken(HttpServletRequest request, HttpServletResponse response);

    /**
     * Logout user by revoking refresh token and clearing cookie.
     *
     * @param request HTTP request to get refresh token from cookie
     * @param response HTTP response for clearing cookies
     */
    void logout(HttpServletRequest request, HttpServletResponse response);
}
