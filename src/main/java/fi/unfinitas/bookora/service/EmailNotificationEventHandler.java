package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.event.SendMailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for SendMailEvent.
 * Handles email sending asynchronously via @Async.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationEventHandler {

    private final EmailTemplateService emailTemplateService;

    /**
     * Handle SendMailEvent asynchronously.
     * Delegates to EmailTemplateService for actual email sending.
     *
     * @param event the email event to handle
     */
    @EventListener
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
