package fi.unfinitas.bookora.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@TestConfiguration
@EnableJpaAuditing
@Import(TestContainersConfiguration.class)
public class RepositoryTestConfiguration {
}
