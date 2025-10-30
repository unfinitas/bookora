package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.enums.UserRole;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.exception.GuestEmailAlreadyRegisteredException;
import fi.unfinitas.bookora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing guest user operations.
 * Handles creation and retrieval of guest users for guest booking functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuestUserService {

    private final UserRepository userRepository;

    /**
     * Find or create a guest user by email.
     * If a guest user with the email exists, return it.
     * If a registered user (isGuest=false) with the email exists, throw exception.
     * Otherwise, create a new guest user.
     *
     * @param email       the guest's email
     * @param firstName   the guest's first name
     * @param lastName    the guest's last name
     * @param phoneNumber the guest's phone number
     * @return the guest user (existing or newly created)
     * @throws GuestEmailAlreadyRegisteredException if email belongs to registered user
     */
    @Transactional
    public User findOrCreateGuestUser(final String email, final String firstName,
                                      final String lastName, final String phoneNumber) {
        log.debug("Processing guest user request");

        final Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            final User user = existingUser.get();

            // If email belongs to a registered user, throw exception
            if (!user.getIsGuest()) {
                log.warn("Email belongs to a registered user. Guest booking not allowed.");
                throw new GuestEmailAlreadyRegisteredException(
                    "This email is already registered. Please log in to make a booking."
                );
            }

            log.debug("Reusing existing guest user with ID: {}", user.getId());
            return user;
        }

        final User guestUser = createGuestUser(email, firstName, lastName, phoneNumber);

        final User savedUser = userRepository.save(guestUser);
        log.debug("Created new guest user with ID: {}", savedUser.getId());

        return savedUser;
    }

    /**
     * Generate a unique username for guest user.
     * Format: "guest_" + emailPrefix + "_" + uniqueSuffix
     *
     * @param email the guest's email
     * @return generated username
     */
    private String generateGuestUsername(final String email) {
        final String emailPrefix = email.split("@")[0];
        final String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        return "guest_" + emailPrefix + "_" + uniqueSuffix;
    }

    private User createGuestUser(final String email, final String firstName, final String lastName, final String phoneNumber) {
        return User.builder()
                .username(generateGuestUsername(email))
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phoneNumber(phoneNumber)
                .isGuest(true)
                .role(UserRole.USER)
                .build();
    }
}
