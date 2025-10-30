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

    /**
     * Convert Booking entity to BookingResponse DTO.
     * Maps customer information and service details.
     *
     * @param booking the booking entity
     * @return BookingResponse DTO
     */
    @Mapping(target = "customerName", expression = "java(booking.getCustomer().getFirstName() + \" \" + booking.getCustomer().getLastName())")
    @Mapping(source = "customer.email", target = "customerEmail")
    @Mapping(source = "customer.phoneNumber", target = "customerPhone")
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    BookingResponse toResponse(Booking booking);

    /**
     * Convert Booking entity and GuestAccessToken to GuestBookingResponse DTO.
     * Includes all booking information plus access token details.
     *
     * @param booking the booking entity
     * @param token   the guest access token
     * @return GuestBookingResponse DTO
     */
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
