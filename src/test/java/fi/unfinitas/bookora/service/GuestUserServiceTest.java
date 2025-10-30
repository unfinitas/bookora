package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.enums.UserRole;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.exception.GuestEmailAlreadyRegisteredException;
import fi.unfinitas.bookora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GuestUserService guestUserService;

    private String testEmail;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    @BeforeEach
    void setUp() {
        testEmail = "guest@example.com";
        firstName = "John";
        lastName = "Doe";
        phoneNumber = "010-1234-5678";
    }

    @Test
    @DisplayName("Should create new guest user when email not exists")
    void shouldCreateNewGuestUserWhenEmailNotExists() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final User result = guestUserService.findOrCreateGuestUser(testEmail, firstName, lastName, phoneNumber);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(testEmail);
        assertThat(result.getFirstName()).isEqualTo(firstName);
        assertThat(result.getLastName()).isEqualTo(lastName);
        assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(result.getIsGuest()).isTrue();
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
        assertThat(result.getUsername()).startsWith("guest_");

        verify(userRepository).findByEmail(testEmail);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should return existing guest user when email exists with isGuest true")
    void shouldReturnExistingGuestUserWhenEmailExistsAsGuest() {
        final User existingGuestUser = User.builder()
                .email(testEmail)
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("010-9876-5432")
                .username("guest_existing")
                .isGuest(true)
                .role(UserRole.USER)
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingGuestUser));

        final User result = guestUserService.findOrCreateGuestUser(testEmail, firstName, lastName, phoneNumber);

        assertThat(result).isSameAs(existingGuestUser);
        assertThat(result.getEmail()).isEqualTo(testEmail);
        assertThat(result.getIsGuest()).isTrue();

        verify(userRepository).findByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email belongs to registered user")
    void shouldThrowExceptionWhenEmailBelongsToRegisteredUser() {
        final User registeredUser = User.builder()
                .email(testEmail)
                .username("registereduser")
                .isGuest(false)
                .role(UserRole.USER)
                .build();

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(registeredUser));

        assertThatThrownBy(() -> guestUserService.findOrCreateGuestUser(testEmail, firstName, lastName, phoneNumber))
                .isInstanceOf(GuestEmailAlreadyRegisteredException.class)
                .hasMessageContaining("This email is already registered. Please log in to make a booking.");

        verify(userRepository).findByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class));
    }
}
