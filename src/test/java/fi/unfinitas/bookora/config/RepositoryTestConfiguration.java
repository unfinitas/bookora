package fi.unfinitas.bookora.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Test configuration for @DataJpaTest repository tests.
 * Enables JPA auditing (createdAt, updatedAt) and imports TestContainers.
 *
 * For @SpringBootTest integration tests, use TestContainersConfiguration only
 * (JpaAuditingConfig from main source is loaded automatically).
 */
@TestConfiguration
@EnableJpaAuditing
@Import(TestContainersConfiguration.class)
public class RepositoryTestConfiguration {
}
