package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.model.ServiceOffering;
import fi.unfinitas.bookora.exception.ServiceOfferingNotFoundException;
import fi.unfinitas.bookora.repository.ServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing ServiceOffering entities.
 * Handles service offering retrieval and validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceOfferingService {

    private final ServiceOfferingRepository serviceOfferingRepository;

    /**
     * Get service offering by ID.
     *
     * @param serviceOfferingId the service offering ID
     * @return the service offering entity
     * @throws ServiceOfferingNotFoundException if service offering not found
     */
    @Transactional(readOnly = true)
    public ServiceOffering getServiceOfferingById(final Long serviceOfferingId) {
        log.debug("Retrieving service offering with ID: {}", serviceOfferingId);

        return serviceOfferingRepository.findById(serviceOfferingId)
                .orElseThrow(() -> {
                    log.warn("Service offering not found with ID: {}", serviceOfferingId);
                    return new ServiceOfferingNotFoundException("Service offering not found with ID: " + serviceOfferingId);
                });
    }
}
