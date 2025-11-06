package fi.unfinitas.bookora.security;

import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.exception.EmailNotVerifiedException;
import fi.unfinitas.bookora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


/**
 * Custom UserDetailsService implementation for loading user-specific data.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        final User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        isGuest(user);

        isEmailVerified(user);

        return new CustomUserDetails(user);
    }

    private static void isEmailVerified(User user) {
        if (!user.getIsEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email to continue");
        }
    }

    private static void isGuest(User user) {
        if (user.getIsGuest()) {
            throw new UsernameNotFoundException("Guest users cannot authenticate");
        }
    }
}
