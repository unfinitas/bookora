package fi.unfinitas.bookora.mapper;

import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.RegisterRequest;
import fi.unfinitas.bookora.dto.response.UserPublicInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for User entity and related DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * Convert RegisterRequest to User entity.
     * Password will be encoded separately in the service layer.
     * createdAt and updatedAt are handled by JPA Auditing.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "isGuest", constant = "false")
    User toEntity(RegisterRequest request);


    /**
     * Convert User entity to UserPublicInfo (simple user data).
     */
    UserPublicInfo toUserResponse(User user);
}
