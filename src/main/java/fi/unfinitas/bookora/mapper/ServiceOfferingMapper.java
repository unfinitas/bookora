package fi.unfinitas.bookora.mapper;

import fi.unfinitas.bookora.domain.model.ServiceOffering;
import fi.unfinitas.bookora.dto.response.ServiceOfferingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for ServiceOffering entity and related DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ServiceOfferingMapper {

    /**
     * Convert ServiceOffering entity to ServiceOfferingResponse DTO.
     * Maps provider's business name to providerBusinessName field.
     *
     * @param serviceOffering the service offering entity
     * @return ServiceOfferingResponse DTO
     */
    @Mapping(source = "provider.businessName", target = "providerBusinessName")
    ServiceOfferingResponse toResponse(ServiceOffering serviceOffering);
}
