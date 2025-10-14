package fi.unfinitas.bookora.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing configuration.
 * Separated from main application class to allow easier testing without JPA.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
