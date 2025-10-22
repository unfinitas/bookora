package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.exception.EmailAlreadyExistsException;
import fi.unfinitas.bookora.exception.UserNotFoundException;
import fi.unfinitas.bookora.exception.UsernameAlreadyExistsException;
import fi.unfinitas.bookora.mapper.UserMapper;
import fi.unfinitas.bookora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing user-related business logic.
 * Handles user creation, validation, and retrieval operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Create a new user from registration request.
     * Validates uniqueness, encodes password, and persists user.
     *
     * @param request the registration request
     * @return the created user
     * @throws EmailAlreadyExistsException if email already exists
     * @throws UsernameAlreadyExistsException if username already exists
     */
    @Transactional
    public User createUser(final RegisterRequest request) {
        log.debug("Creating new user");

        validateEmailNotExists(request.getEmail());
        validateUsernameNotExists(request.getUsername());

        final User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        final User savedUser = userRepository.save(user);

        log.debug("User created successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public User findByUsername(final String username) {
        log.debug("Finding user by username");
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found");
                    return new UserNotFoundException("User not found with username: " + username);
                });
    }

    @Transactional(readOnly = true)
    public User findByEmail(final String email) {
        log.debug("Finding user by email");
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found");
                    return new UserNotFoundException("User not found with email: " + email);
                });
    }

    @Transactional(readOnly = true)
    public User findById(final UUID id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });
    }


    /**
     * Update the last verification email sent timestamp for a user.
     *
     * @param userId the user ID
     */
    @Transactional
    public void updateLastVerificationEmailSentAt(final UUID userId) {
        log.debug("Updating last verification email sent timestamp for user ID: {}", userId);
        final User user = findById(userId);
        user.setLastVerificationEmailSentAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Validate that email doesn't already exist.
     *
     * @param email the email to validate
     * @throws EmailAlreadyExistsException if email already exists
     */
    private void validateEmailNotExists(final String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Email already exists");
            throw new EmailAlreadyExistsException("Email is already registered: " + email);
        }
    }

    /**
     * Validate that username doesn't already exist.
     *
     * @param username the username to validate
     * @throws UsernameAlreadyExistsException if username already exists
     */
    private void validateUsernameNotExists(final String username) {
        if (userRepository.existsByUsername(username)) {
            log.warn("Username already exists");
            throw new UsernameAlreadyExistsException("Username is already taken: " + username);
        }
    }
}
