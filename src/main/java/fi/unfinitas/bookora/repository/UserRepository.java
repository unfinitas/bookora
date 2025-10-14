package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if a user with the given email exists.
     *
     * @param email the email to check
     * @return true if a user with this email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if a user with the given username exists.
     *
     * @param username the username to check
     * @return true if a user with this username exists
     */
    boolean existsByUsername(String username);
}
