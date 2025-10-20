package fi.unfinitas.bookora.domain.event;

import java.util.Map;

/**
 * Domain event for sending email notifications.
 * Published by BookingService when email needs to be sent.
 * Handled asynchronously by EmailNotificationEventHandler.
 */
public record SendMailEvent(
    String to,
    String subject,
    String templateName,
    Map<String, Object> templateVariables
) {}
