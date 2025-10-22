package fi.unfinitas.bookora.mapper;

import fi.unfinitas.bookora.domain.model.Booking;
import fi.unfinitas.bookora.domain.model.GuestAccessToken;
import fi.unfinitas.bookora.dto.response.BookingResponse;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * Mapper for Booking entity and related DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = ServiceOfferingMapper.class)
public interface BookingMapper {

    @Mapping(target = "customerName", expression = "java(booking.getCustomer().getFirstName() + \" \" + booking.getCustomer().getLastName())")
    @Mapping(source = "customer.email", target = "customerEmail")
    @Mapping(source = "customer.phoneNumber", target = "customerPhone")
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    BookingResponse toResponse(Booking booking);

    @Mapping(target = "customerName", expression = "java(booking.getCustomer().getFirstName() + \" \" + booking.getCustomer().getLastName())")
    @Mapping(source = "booking.customer.email", target = "customerEmail")
    @Mapping(source = "booking.customer.phoneNumber", target = "customerPhone")
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    @Mapping(source = "booking.serviceOffering", target = "serviceOffering")
    @Mapping(source = "booking.id", target = "id")
    @Mapping(source = "booking.startTime", target = "startTime")
    @Mapping(source = "booking.endTime", target = "endTime")
    @Mapping(source = "booking.notes", target = "notes")
    @Mapping(source = "booking.createdAt", target = "createdAt")
    @Mapping(source = "token.token", target = "accessToken")
    @Mapping(source = "token.expiresAt", target = "tokenExpiresAt")
    GuestBookingResponse toGuestResponse(Booking booking, GuestAccessToken token);
}
