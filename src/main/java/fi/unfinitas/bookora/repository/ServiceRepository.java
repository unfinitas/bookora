package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for Service entity.
 */
public interface ServiceRepository extends JpaRepository<Service, Long> {
}
