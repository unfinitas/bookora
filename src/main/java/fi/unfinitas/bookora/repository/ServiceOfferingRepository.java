package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for ServiceOffering entity.
 */
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
}
