package fi.unfinitas.bookora.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for rate limiting and other resilience patterns.
 * Configures rate limiters for email verification resend functionality.
 */
@Configuration
public class Resilience4jConfig {

    /**
     * Configure RateLimiterRegistry with custom settings.
     * Email verification resend is limited to 1 request per hour per email.
     *
     * @return configured RateLimiterRegistry
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        final RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofHours(1))
                .timeoutDuration(Duration.ZERO)
                .build();

        return RateLimiterRegistry.of(config);
    }
}
