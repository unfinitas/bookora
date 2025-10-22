package fi.unfinitas.bookora.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for Bookora application.
 * Binds to 'bookora' prefix in application.yml.
 */
@ConfigurationProperties(prefix = "bookora")
@Validated
@Getter
@Setter
@Configuration
public class BookoraProperties {

    /**
     * Frontend application URL (used for redirects after verification).
     */
    @NotBlank(message = "Frontend URL must not be blank")
    private String frontendUrl;

    /**
     * Backend application URL (used in email verification links).
     */
    @NotBlank(message = "Backend URL must not be blank")
    private String backendUrl;

    /**
     * Email configuration.
     */
    private Email email = new Email();

    /**
     * Guest user configuration.
     */
    private Guest guest = new Guest();

    @Getter
    @Setter
    public static class Email {
        /**
         * Email sender address (From field).
         */
        @NotBlank(message = "Email from address must not be blank")
        private String from;

        /**
         * Enable/disable email sending (useful for testing).
         */
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Guest {
        /**
         * Guest access token configuration.
         */
        private Token token = new Token();

        /**
         * Guest booking configuration.
         */
        private Booking booking = new Booking();

        @Getter
        @Setter
        public static class Token {
            @Min(value = 0, message = "Token expiration extension days must be at least 0")
            private int expirationExtensionDays = 30;
        }

        @Getter
        @Setter
        public static class Booking {
            /**
             * Hours before booking start time when cancellation is no longer allowed.
             * Default: 24 hours
             */
            @Min(value = 1, message = "Cancellation window must be at least 1 hour")
            private int cancellationWindowHours = 24;
        }
    }

    @Getter
    @Setter
    public static class Verification {
        /**
         * Email verification token configuration.
         */
        private Token token = new Token();

        @Getter
        @Setter
        public static class Token {
            /**
             * Token expiration in days.
             */
            @Min(value = 1, message = "Token expiration days must be at least 1")
            private int expirationDays = 7;
        }
    }

    private Verification verification = new Verification();
}
