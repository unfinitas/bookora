package fi.unfinitas.bookora.mapper;

import fi.unfinitas.bookora.domain.model.Service;
import fi.unfinitas.bookora.dto.response.ServiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for Service entity and related DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ServiceMapper {

    /**
     * Convert Service entity to ServiceResponse DTO.
     * Maps provider's business name to providerBusinessName field.
     *
     * @param service the service entity
     * @return ServiceResponse DTO
     */
    @Mapping(source = "provider.businessName", target = "providerBusinessName")
    ServiceResponse toResponse(Service service);
}
