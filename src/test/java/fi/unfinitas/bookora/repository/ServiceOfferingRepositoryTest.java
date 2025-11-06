package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.config.RepositoryTestConfiguration;
import fi.unfinitas.bookora.domain.model.ServiceOffering;
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
class ServiceOfferingRepositoryTest {

    @Autowired
    private ServiceOfferingRepository serviceOfferingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findById_ExistingServiceOffering_ReturnsServiceOffering() {
        // GIVEN: ServiceOffering exists in database
        // First persist User, then Provider, then ServiceOffering
        final var user = TestDataBuilder.user().build();
        entityManager.persist(user);

        final var provider = TestDataBuilder.provider().user(user).build();
        entityManager.persist(provider);

        final ServiceOffering serviceOffering = TestDataBuilder.serviceOffering().provider(provider).build();
        final ServiceOffering saved = entityManager.persistAndFlush(serviceOffering);

        // WHEN: findById is called
        final Optional<ServiceOffering> result = serviceOfferingRepository.findById(saved.getId());

        // THEN: ServiceOffering is returned
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Haircut");
    }

    @Test
    void findById_NonExistentServiceOffering_ReturnsEmpty() {
        // GIVEN: ServiceOffering ID does not exist
        // WHEN: findById is called
        final Optional<ServiceOffering> result = serviceOfferingRepository.findById(999L);

        // THEN: Optional.empty() is returned
        assertThat(result).isEmpty();
    }
}
