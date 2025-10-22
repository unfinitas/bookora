package fi.unfinitas.bookora.service.impl;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.enums.BookingStatus;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import fi.unfinitas.bookora.domain.model.Booking;
import fi.unfinitas.bookora.domain.model.GuestAccessToken;
import fi.unfinitas.bookora.domain.model.Service;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;
import fi.unfinitas.bookora.dto.response.BookingResponse;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;
import fi.unfinitas.bookora.exception.BookingAlreadyCancelledException;
import fi.unfinitas.bookora.exception.BookingAlreadyConfirmedException;
import fi.unfinitas.bookora.exception.BookingNotFoundException;
import fi.unfinitas.bookora.exception.CannotCancelBookingException;
import fi.unfinitas.bookora.exception.InvalidBookingTimeException;
import fi.unfinitas.bookora.exception.InvalidTokenException;
import fi.unfinitas.bookora.mapper.BookingMapper;
import fi.unfinitas.bookora.repository.BookingRepository;
import fi.unfinitas.bookora.service.BookingService;
import fi.unfinitas.bookora.service.GuestAccessTokenService;
import fi.unfinitas.bookora.service.GuestUserService;
import fi.unfinitas.bookora.service.ServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of BookingService for managing booking operations.
 * Handles guest booking creation, retrieval, and cancellation.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceService serviceService;
    private final GuestUserService guestUserService;
    private final GuestAccessTokenService tokenService;
    private final BookingMapper bookingMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final BookoraProperties bookoraProperties;

    @Override
    @Transactional
    public GuestBookingResponse createGuestBooking(final CreateGuestBookingRequest request) {
        log.debug("Creating guest booking for service ID: {}", request.getServiceId());

        validateBookingTimes(request.getStartTime(), request.getEndTime());

        final Service service = serviceService.getServiceById(request.getServiceId());

        final UUID providerId = service.getProvider().getId();
        final boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                providerId,
                request.getStartTime(),
                request.getEndTime()
        );

        hasOverlap(request, hasOverlap, providerId);

        final User guestUser = guestUserService.findOrCreateGuestUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
        );

        final Booking booking = Booking.builder()
                .customer(guestUser)
                .provider(service.getProvider())
                .service(service)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.PENDING)
                .notes(request.getNotes())
                .build();

        final Booking savedBooking = bookingRepository.save(booking);
        log.debug("Booking created successfully with ID: {}", savedBooking.getId());

        final GuestAccessToken token = tokenService.generateToken(savedBooking);

        final GuestBookingResponse response = bookingMapper.toGuestResponse(savedBooking, token);
        log.debug("Guest booking completed. Booking ID: {}", savedBooking.getId());

        publishSendMailEvent(response, service, savedBooking);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByToken(final UUID token) {
        log.debug("Retrieving booking by access token");

        try {
            final GuestAccessToken accessToken = tokenService.validateToken(token);
            final Booking booking = accessToken.getBooking();
            final BookingResponse response = bookingMapper.toResponse(booking);

            log.debug("Booking retrieved successfully. ID: {} Status: {}", booking.getId(), booking.getStatus());
            return response;
        } catch (final InvalidTokenException e) {
            // Convert InvalidTokenException to BookingNotFoundException for GET endpoints
            log.warn("Invalid token provided for booking retrieval: {}", e.getMessage());
            throw new BookingNotFoundException("Booking not found with the provided token");
        }
    }

    private static void hasOverlap(CreateGuestBookingRequest request, boolean hasOverlap, UUID providerId) {
        if (hasOverlap) {
            log.warn("Overlapping booking detected for provider {} at time range {} - {}",
                    providerId, request.getStartTime(), request.getEndTime());
            throw new InvalidBookingTimeException(
                    "The selected time slot is already booked. Please choose another time."
            );
        }
    }

    @Override
    @Transactional
    public BookingResponse confirmBookingByToken(final UUID token) {
        log.debug("Confirming booking");

        final GuestAccessToken accessToken = tokenService.validateToken(token);
        final Booking booking = accessToken.getBooking();

        validateBooking(booking);

        booking.setStatus(BookingStatus.CONFIRMED);
        final Booking updatedBooking = bookingRepository.save(booking);

        accessToken.markAsConfirmed();

        log.debug("Booking confirmed successfully. ID: {}", updatedBooking.getId());
        return bookingMapper.toResponse(updatedBooking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByToken(final UUID token) {
        log.debug("Cancelling booking");

        final GuestAccessToken accessToken = tokenService.validateToken(token);
        final Booking booking = accessToken.getBooking();

        validateStatus(booking);

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime cancellationDeadline = booking.getStartTime().minusHours(24);

        if (now.isAfter(cancellationDeadline)) {
            log.warn("Cannot cancel booking {} within 24 hours of start time. Deadline: {}, Now: {}",
                    booking.getId(), cancellationDeadline, now);
            throw new CannotCancelBookingException("Cannot cancel booking within 24 hours of start time.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        final Booking updatedBooking = bookingRepository.save(booking);

        log.debug("Booking cancelled successfully. ID: {}", updatedBooking.getId());

        try {
            final BookingResponse response = bookingMapper.toResponse(updatedBooking);
            final Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("booking", response);
            templateVariables.put("frontendUrl", bookoraProperties.getFrontendUrl());

            final SendMailEvent event = new SendMailEvent(
                    response.customerEmail(),
                    "Booking Cancelled - " + updatedBooking.getService().getName(),
                    "email/booking-cancelled",
                    templateVariables
            );

            eventPublisher.publishEvent(event);
            log.debug("Published SendMailEvent for cancelled booking ID: {}", updatedBooking.getId());
        } catch (final Exception e) {
            log.error("Failed to publish SendMailEvent for cancelled booking ID: {}", updatedBooking.getId(), e);
        }

        return bookingMapper.toResponse(updatedBooking);
    }

    @Override
    @Transactional
    public BookingResponse validateAndConfirmBooking(final UUID token) {
        log.debug("Validating token and auto-confirming booking if pending");

        final GuestAccessToken accessToken = tokenService.validateToken(token);
        final Booking booking = accessToken.getBooking();

        log.debug("Token validated. Booking ID: {}, Status: {}", booking.getId(), booking.getStatus());

        if (booking.getStatus() == BookingStatus.PENDING) {
            log.debug("Booking is PENDING. Auto-confirming to CONFIRMED");
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            // Mark token as confirmed (JPA will auto-persist the change)
            accessToken.markAsConfirmed();

            log.info("Booking auto-confirmed successfully. ID: {}", booking.getId());
        } else {
            // If already CONFIRMED or CANCELLED, return current status (idempotent)
            log.debug("Booking status is {}. Returning current status without modification", booking.getStatus());
        }

        return bookingMapper.toResponse(booking);
    }

    private void publishSendMailEvent(final GuestBookingResponse response, final Service service, final Booking savedBooking) {
        try {
            final Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("booking", response);
            templateVariables.put("frontendUrl", bookoraProperties.getFrontendUrl());

            final SendMailEvent event = new SendMailEvent(
                    response.customerEmail(),
                    "Booking Confirmation - " + service.getName(),
                    "email/booking-created",
                    templateVariables
            );

            eventPublisher.publishEvent(event);
            log.debug("Published SendMailEvent for booking ID: {}", savedBooking.getId());
        } catch (final Exception e) {
            log.error("Failed to publish SendMailEvent for booking ID: {}", savedBooking.getId(), e);
            // Don't fail booking creation if event publishing fails
        }
    }

    private static void validateStatus(final Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Booking {} is already cancelled", booking.getId());
            throw new CannotCancelBookingException("Booking has already been cancelled");
        }
    }

    private static void validateBooking(final Booking booking) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.warn("Booking {} is already confirmed", booking.getId());
            throw new BookingAlreadyConfirmedException("Booking has already been confirmed");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Cannot confirm cancelled booking {}", booking.getId());
            throw new BookingAlreadyCancelledException("Cannot confirm a cancelled booking");
        }

        if (LocalDateTime.now().isAfter(booking.getStartTime())) {
            log.warn("Cannot confirm booking {} that has already started", booking.getId());
            throw new InvalidBookingTimeException("Cannot confirm a booking that has already started");
        }
    }

    private void validateBookingTimes(final LocalDateTime startTime, final LocalDateTime endTime) {
        final LocalDateTime now = LocalDateTime.now();

        if (startTime.isBefore(now)) {
            log.warn("Start time {} is in the past", startTime);
            throw new InvalidBookingTimeException("Booking start time cannot be in the past");
        }

        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            log.warn("End time {} is before or equal to start time {}", endTime, startTime);
            throw new InvalidBookingTimeException("Booking end time must be after start time");
        }

        log.debug("Booking times validated: {} - {}", startTime, endTime);
    }
}
