package fi.unfinitas.bookora.repository;

import fi.unfinitas.bookora.domain.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {
}
