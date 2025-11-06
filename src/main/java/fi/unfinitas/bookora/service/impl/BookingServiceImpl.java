package fi.unfinitas.bookora.service.impl;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.enums.BookingStatus;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import fi.unfinitas.bookora.domain.model.Booking;
import fi.unfinitas.bookora.domain.model.GuestAccessToken;
import fi.unfinitas.bookora.domain.model.ServiceOffering;
import fi.unfinitas.bookora.domain.model.User;
import fi.unfinitas.bookora.dto.request.CreateGuestBookingRequest;
import fi.unfinitas.bookora.dto.response.BookingResponse;
import fi.unfinitas.bookora.dto.response.GuestBookingResponse;
import fi.unfinitas.bookora.exception.BookingAlreadyCancelledException;
import fi.unfinitas.bookora.exception.BookingAlreadyConfirmedException;
import fi.unfinitas.bookora.exception.CannotCancelBookingException;
import fi.unfinitas.bookora.exception.CustomerBookingConflictException;
import fi.unfinitas.bookora.exception.InvalidBookingTimeException;
import fi.unfinitas.bookora.mapper.BookingMapper;
import fi.unfinitas.bookora.repository.BookingRepository;
import fi.unfinitas.bookora.service.BookingService;
import fi.unfinitas.bookora.service.GuestAccessTokenService;
import fi.unfinitas.bookora.service.GuestUserService;
import fi.unfinitas.bookora.service.ServiceOfferingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final ServiceOfferingService serviceOfferingService;
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

        final ServiceOffering serviceOffering = serviceOfferingService.getServiceOfferingById(request.getServiceId());

        final UUID providerId = serviceOffering.getProvider().getId();
        final boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                providerId,
                request.getStartTime(),
                request.getEndTime()
        );

        if (hasOverlap) {
            log.warn("Overlapping booking detected for provider {} at time range {} - {}",
                    providerId, request.getStartTime(), request.getEndTime());
            throw new InvalidBookingTimeException(
                    "The selected time slot is already booked. Please choose another time."
            );
        }

        final User guestUser = guestUserService.findOrCreateGuestUser(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                request.getPhoneNumber()
        );

        final boolean hasCustomerOverlap = bookingRepository.existsCustomerOverlappingBooking(
                guestUser.getId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (hasCustomerOverlap) {
            log.warn("Customer {} already has a booking during {} - {}",
                    guestUser.getEmail(), request.getStartTime(), request.getEndTime());
            throw new CustomerBookingConflictException(
                    "You already have a booking during this time. You cannot book multiple appointments at the same time."
            );
        }

        final Booking booking = Booking.builder()
                .customer(guestUser)
                .provider(serviceOffering.getProvider())
                .serviceOffering(serviceOffering)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.PENDING)
                .notes(request.getNotes())
                .build();

        try {
            final Booking savedBooking = bookingRepository.save(booking);
            log.debug("Booking created successfully with ID: {}", savedBooking.getId());

            final GuestAccessToken token = tokenService.generateToken(savedBooking);

            final GuestBookingResponse response = bookingMapper.toGuestResponse(savedBooking, token);
            log.debug("Guest booking completed. Booking ID: {}", savedBooking.getId());

            publishSendMailEvent(response, serviceOffering, savedBooking);

            return response;
        } catch (DataIntegrityViolationException e) {
            if (isProviderOverlapConstraint(e)) {
                log.warn("Race condition: provider booking overlap for provider {} at {} - {}",
                        providerId, request.getStartTime(), request.getEndTime());
                throw new InvalidBookingTimeException(
                        "This time slot was just booked by another user. Please select a different time."
                );
            }
            if (isCustomerOverlapConstraint(e)) {
                log.warn("Race condition: customer booking overlap for customer {} at {} - {}",
                        guestUser.getEmail(), request.getStartTime(), request.getEndTime());
                throw new CustomerBookingConflictException(
                        "You already have a booking during this time. You cannot book multiple appointments at the same time."
                );
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByToken(final UUID token) {
        log.debug("Retrieving booking by access token");

        final GuestAccessToken accessToken = tokenService.validateToken(token);
        final Booking booking = accessToken.getBooking();

        log.debug("Booking retrieved successfully. ID: {} Status: {}", booking.getId(), booking.getStatus());
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse confirmBookingByToken(final UUID token) {
        log.debug("Confirming booking");

        final GuestAccessToken accessToken = tokenService.validateTokenForConfirm(token);
        final Booking booking = accessToken.getBooking();

        validateBooking(booking);

        booking.setStatus(BookingStatus.CONFIRMED);

        accessToken.markAsConfirmed();

        log.debug("Booking confirmed successfully. ID: {}", booking.getId());
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByToken(final UUID token) {
        log.debug("Cancelling booking");

        final GuestAccessToken accessToken = tokenService.validateToken(token);
        final Booking booking = accessToken.getBooking();

        validateStatus(booking);

        // Check if cancellation is within configured hours of booking start time
        final int cancellationWindowHours = bookoraProperties.getGuest().getBooking().getCancellationWindowHours();
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime cancellationDeadline = booking.getStartTime().minusHours(cancellationWindowHours);

        if (now.isAfter(cancellationDeadline)) {
            log.warn("Cannot cancel booking {} within {} hours of start time. Deadline: {}, Now: {}",
                    booking.getId(), cancellationWindowHours, cancellationDeadline, now);
            throw new CannotCancelBookingException(
                    String.format("Cannot cancel booking within %d hours of start time.", cancellationWindowHours)
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        final Booking updatedBooking = bookingRepository.save(booking);

        // Revoke token to prevent further access
        accessToken.softDelete("BOOKING_CANCELLED");

        log.debug("Booking cancelled successfully. ID: {}", updatedBooking.getId());

        // Publish SendMailEvent for booking cancellation email
        try {
            final BookingResponse response = bookingMapper.toResponse(updatedBooking);
            final Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("booking", response);
            templateVariables.put("frontendUrl", bookoraProperties.getFrontendUrl());

            final SendMailEvent event = new SendMailEvent(
                    response.customerEmail(),
                    "Booking Cancelled - " + updatedBooking.getServiceOffering().getName(),
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

    private void publishSendMailEvent(final GuestBookingResponse response, final ServiceOffering serviceOffering, final Booking savedBooking) {
        try {
            final Map<String, Object> templateVariables = new HashMap<>();
            templateVariables.put("booking", response);
            templateVariables.put("frontendUrl", bookoraProperties.getFrontendUrl());

            final SendMailEvent event = new SendMailEvent(
                    response.customerEmail(),
                    "Booking Confirmation - " + serviceOffering.getName(),
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

    private boolean isProviderOverlapConstraint(DataIntegrityViolationException e) {
        final Throwable rootCause = e.getRootCause();
        if (rootCause == null) {
            return false;
        }
        final String message = rootCause.getMessage();
        return message != null && message.contains("idx_no_overlapping_bookings");
    }

    private boolean isCustomerOverlapConstraint(DataIntegrityViolationException e) {
        final Throwable rootCause = e.getRootCause();
        if (rootCause == null) {
            return false;
        }
        final String message = rootCause.getMessage();
        return message != null && message.contains("idx_no_overlapping_customer_bookings");
    }
}
