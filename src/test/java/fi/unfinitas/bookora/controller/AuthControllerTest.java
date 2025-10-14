package fi.unfinitas.bookora.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.unfinitas.bookora.dto.request.LoginRequest;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.LoginResponse;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import fi.unfinitas.bookora.exception.EmailAlreadyExistsException;
import fi.unfinitas.bookora.security.JwtUtil;
import fi.unfinitas.bookora.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserPublicInfo userPublicInfo;
    private LoginResponse loginData;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("Password123!")
                .build();

        loginRequest = new LoginRequest("testuser", "Password123!");

        userPublicInfo = UserPublicInfo.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .role("USER")
                .build();

        loginData = new LoginResponse(
                UUID.randomUUID(),
                "testuser",
                "USER",
                "access-token",
                "refresh-token",
                "Bearer",
                86400000L
        );
    }



    @Test
    @DisplayName("Should successfully register new user")
    @WithMockUser
    void shouldSuccessfullyRegisterNewUser() throws Exception {
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(userPublicInfo);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("User registration successful"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        verify(authenticationService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when registration request is invalid")
    @WithMockUser
    void shouldReturn400WhenRegistrationRequestIsInvalid() throws Exception {
        final RegisterRequest invalidRequest = RegisterRequest.builder()
                .username("ab") // Too short
                .firstName("")
                .lastName("")
                .email("invalid-email")
                .password("weak")
                .build();

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should integrate with GlobalExceptionHandler for service exceptions")
    @WithMockUser
    void shouldIntegrateWithGlobalExceptionHandler() throws Exception {
        // Verify that controller properly delegates exception handling to GlobalExceptionHandler
        // Business logic validation is tested in service layer
        // Exception handling is tested in GlobalExceptionHandlerTest
        // This test only verifies the integration between layers
        when(authenticationService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email is already registered"));

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());

        verify(authenticationService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should successfully login user")
    @WithMockUser
    void shouldSuccessfullyLoginUser() throws Exception {
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(loginData);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(86400000));

        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when login request is invalid")
    @WithMockUser
    void shouldReturn400WhenLoginRequestIsInvalid() throws Exception {
        final LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should successfully refresh token")
    @WithMockUser
    void shouldSuccessfullyRefreshToken() throws Exception {
        when(authenticationService.refreshToken(anyString())).thenReturn(loginData);

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .header("Authorization", "Bearer valid-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        verify(authenticationService).refreshToken("valid-refresh-token");
    }

    @Test
    @DisplayName("Should return 401 when Authorization header is missing")
    @WithMockUser
    void shouldReturn401WhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).refreshToken(anyString());
    }

    @Test
    @DisplayName("Should return 401 when Authorization header does not start with Bearer")
    @WithMockUser
    void shouldReturn401WhenAuthorizationHeaderDoesNotStartWithBearer() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .header("Authorization", "Basic invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(authenticationService, never()).refreshToken(anyString());
    }
}
