package fi.unfinitas.bookora.security;

import fi.unfinitas.bookora.domain.enums.UserRole;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .password("encodedPassword123")
                .role(UserRole.USER)
                .isGuest(false)
                .isEmailVerified(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully load user by username")
    void shouldSuccessfullyLoadUserByUsername() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        final UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword123");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username: nonexistent");
    }

    @Test
    @DisplayName("Should throw exception when user is guest")
    void shouldThrowExceptionWhenUserIsGuest() {
        final User guestUser = User.builder()
                .id(UUID.randomUUID())
                .username("guestuser")
                .firstName("Guest")
                .lastName("User")
                .email("guest@example.com")
                .password(null)
                .role(UserRole.USER)
                .isGuest(true)
                .build();

        when(userRepository.findByUsername("guestuser")).thenReturn(Optional.of(guestUser));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("guestuser"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Guest users cannot authenticate");
    }

    @Test
    @DisplayName("Should load user with PROVIDER role")
    void shouldLoadUserWithProviderRole() {
        final User providerUser = User.builder()
                .id(UUID.randomUUID())
                .username("provider")
                .firstName("Provider")
                .lastName("User")
                .email("provider@example.com")
                .password("encodedPassword123")
                .role(UserRole.PROVIDER)
                .isGuest(false)
                .isEmailVerified(true)
                .build();

        when(userRepository.findByUsername("provider")).thenReturn(Optional.of(providerUser));

        final UserDetails userDetails = customUserDetailsService.loadUserByUsername("provider");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_PROVIDER");
    }

    @Test
    @DisplayName("Should handle null username gracefully")
    void shouldHandleNullUsernameGracefully() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle empty username")
    void shouldHandleEmptyUsername() {
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username:");
    }
}
