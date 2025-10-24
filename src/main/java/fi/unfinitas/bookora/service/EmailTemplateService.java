package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Email template service with rendering and retry logic.
 * Renders Thymeleaf templates and sends emails with automatic retry on failure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;
    private final JavaMailSender javaMailSender;
    private final BookoraProperties bookoraProperties;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second

    /**
     * Send email with retry logic (3 attempts with exponential backoff).
     *
     * @param event the email event containing recipient, subject, template, and variables
     */
    public void send(final SendMailEvent event) {
        try {
            final String htmlContent = renderTemplate(event.templateName(), event.templateVariables());

            final MimeMessage mimeMessage = createMimeMessage(event.to(), event.subject(), htmlContent);

            sendWithRetry(mimeMessage);

            log.info("Email sent successfully to: {}", event.to());
        } catch (final Exception e) {
            log.error("Failed to send email to {} after {} attempts: {}",
                event.to(), MAX_RETRY_ATTEMPTS, e.getMessage(), e);
            // Don't propagate exception - email failure should not fail business logic
        }
    }

    /**
     * Render Thymeleaf template with variables.
     */
    private String renderTemplate(final String templateName, final java.util.Map<String, Object> variables) {
        try {
            final Context context = new Context();
            context.setVariables(variables);
            return templateEngine.process(templateName, context);
        } catch (final Exception e) {
            log.error("Failed to render template {}: {}", templateName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create MimeMessage with HTML content.
     */
    private MimeMessage createMimeMessage(final String to, final String subject, final String htmlContent) throws MessagingException {
        final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(bookoraProperties.getEmail().getFrom());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = HTML

        return mimeMessage;
    }

    /**
     * Send email with retry logic (exponential backoff).
     */
    private void sendWithRetry(final MimeMessage mimeMessage) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                javaMailSender.send(mimeMessage);
                if (attempt > 0) {
                    log.info("Email sent successfully on retry attempt {}", attempt + 1);
                }
                return; // Success
            } catch (final MailException e) {
                attempt++;
                lastException = e;

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    final long backoffMs = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    log.warn("Email send failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt, MAX_RETRY_ATTEMPTS, backoffMs, e.getMessage());

                    try {
                        Thread.sleep(backoffMs);
                    } catch (final InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                } else {
                    log.error("Email send failed after {} attempts", MAX_RETRY_ATTEMPTS);
                }
            }
        }

        // All retries exhausted
        throw new RuntimeException("Failed to send email after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }
}
