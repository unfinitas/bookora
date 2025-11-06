package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.event.SendMailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for SendMailEvent.
 * Handles email sending asynchronously AFTER transaction commits.
 * This ensures emails are only sent when the booking is successfully saved.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationEventHandler {

    private final EmailTemplateService emailTemplateService;

    /**
     * Handle SendMailEvent asynchronously AFTER transaction commits.
     * This prevents sending confirmation emails for bookings that fail to save.
     *
     * @param event the email event to handle
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailTaskExecutor")
    public void handleSendMailEvent(final SendMailEvent event) {
        if (event == null) {
            log.warn("Received null SendMailEvent, skipping email send");
            return;
        }

        try {
            log.debug("Handling SendMailEvent for recipient: {}", event.to());
            emailTemplateService.send(event);
        } catch (final Exception e) {
            log.error("Failed to handle SendMailEvent for recipient {}: {}",
                event.to(), e.getMessage(), e);
        }
    }
}
