package fi.unfinitas.bookora.config;

import fi.unfinitas.bookora.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled tasks configuration.
 * Handles periodic cleanup operations like expired token removal.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final RefreshTokenService refreshTokenService;

    /**
     * Clean up expired refresh tokens daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredTokens() {
        try {
            log.info("Starting scheduled cleanup of expired refresh tokens");
            refreshTokenService.cleanupExpiredTokens();
            log.info("Completed scheduled cleanup of expired refresh tokens");
        } catch (Exception e) {
            log.error("Error during scheduled cleanup of expired refresh tokens", e);
        }
    }
}