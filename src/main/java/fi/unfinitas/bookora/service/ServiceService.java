package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.model.Service;
import fi.unfinitas.bookora.exception.ServiceNotFoundException;
import fi.unfinitas.bookora.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing Service entities.
 * Handles service retrieval and validation.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ServiceService {

    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public Service getServiceById(final Long serviceId) {
        log.debug("Retrieving service with ID: {}", serviceId);

        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> {
                    log.warn("Service not found with ID: {}", serviceId);
                    return new ServiceNotFoundException("Service not found with ID: " + serviceId);
                });
    }
}
