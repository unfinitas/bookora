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
        log.info("Creating new user with username: {}", request.getUsername());

        validateEmailNotExists(request.getEmail());
        validateUsernameNotExists(request.getUsername());

        final User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        final User savedUser = userRepository.save(user);

        log.info("User created successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    /**
     * Find user by username.
     *
     * @param username the username to search for
     * @return the user
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findByUsername(final String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username: " + username);
                });
    }

    /**
     * Find user by email.
     *
     * @param email the email to search for
     * @return the user
     * @throws UserNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User findByEmail(final String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    return new UserNotFoundException("User not found with email: " + email);
                });
    }

    /**
     * Find user by ID.
     *
     * @param id the user ID
     * @return the user
     * @throws UserNotFoundException if user not found
     */
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
     * Validate that email doesn't already exist.
     *
     * @param email the email to validate
     * @throws EmailAlreadyExistsException if email already exists
     */
    private void validateEmailNotExists(final String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Email already exists: {}", email);
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
            log.warn("Username already exists: {}", username);
            throw new UsernameAlreadyExistsException("Username is already taken: " + username);
        }
    }
}
