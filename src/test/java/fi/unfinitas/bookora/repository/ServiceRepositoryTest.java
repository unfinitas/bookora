package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.model.Service;
import fi.unfinitas.bookora.testutil.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(RepositoryTestConfiguration.class)
@ActiveProfiles("test")
class ServiceRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findById_ExistingService_ReturnsService() {
        // GIVEN: Service exists in database
        // First persist User, then Provider, then Service
        final var user = TestDataBuilder.user().build();
        entityManager.persist(user);

        final var provider = TestDataBuilder.provider().user(user).build();
        entityManager.persist(provider);

        final Service service = TestDataBuilder.service().provider(provider).build();
        final Service saved = entityManager.persistAndFlush(service);

        // WHEN: findById is called
        final Optional<Service> result = serviceRepository.findById(saved.getId());

        // THEN: Service is returned
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Haircut");
    }

    @Test
    void findById_NonExistentService_ReturnsEmpty() {
        // GIVEN: Service ID does not exist
        // WHEN: findById is called
        final Optional<Service> result = serviceRepository.findById(999L);

        // THEN: Optional.empty() is returned
        assertThat(result).isEmpty();
    }
}
