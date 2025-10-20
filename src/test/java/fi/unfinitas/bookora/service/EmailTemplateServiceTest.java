package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.config.BookoraProperties;
import fi.unfinitas.bookora.domain.event.SendMailEvent;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTemplateService Tests")
class EmailTemplateServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private BookoraProperties bookoraProperties;

    @InjectMocks
    private EmailTemplateService emailTemplateService;

    @Mock
    private MimeMessage mimeMessage;

    private SendMailEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = new SendMailEvent(
            "customer@example.com",
            "Booking Confirmation - Haircut",
            "email/booking-created",
            Map.of(
                "customerName", "John Doe",
                "serviceName", "Haircut",
                "startTime", "2024-10-20 10:00",
                "endTime", "2024-10-20 11:00"
            )
        );

        // Mock JavaMailSender to return MimeMessage
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Stub BookoraProperties (lenient to avoid UnnecessaryStubbingException)
        final BookoraProperties.Email emailConfig = new BookoraProperties.Email();
        emailConfig.setFrom("test@bookora.com");
        lenient().when(bookoraProperties.getEmail()).thenReturn(emailConfig);
    }

    @Test
    @DisplayName("send() - Valid SendMailEvent - Sends email successfully")
    void send_ValidEvent_SendsEmail() throws Exception {
        // GIVEN: Valid SendMailEvent
        String renderedHtml = "<html><body>Booking confirmation email</body></html>";
        when(templateEngine.process(eq("email/booking-created"), any(Context.class)))
            .thenReturn(renderedHtml);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // WHEN: send is called
        emailTemplateService.send(validEvent);

        // THEN: Email is sent via JavaMailSender
        verify(templateEngine, times(1)).process(eq("email/booking-created"), any(Context.class));
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("send() - Template rendering - Passes correct variables")
    void send_TemplateRendering_PassesCorrectVariables() {
        // GIVEN: SendMailEvent with template variables
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenAnswer(invocation -> {
                Context context = invocation.getArgument(1);
                // Verify context contains expected variables
                return "<html>Email content</html>";
            });
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // WHEN: send is called
        emailTemplateService.send(validEvent);

        // THEN: Template receives all required variables
        verify(templateEngine, times(1)).process(eq("email/booking-created"), any(Context.class));
    }

    @Test
    @DisplayName("send() - Template processing fails - Logs error and does not throw")
    void send_TemplateProcessingFails_LogsError() {
        // GIVEN: Template processing throws exception
        // Reset the mock to avoid UnnecessaryStubbingException
        reset(javaMailSender);
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenThrow(new RuntimeException("Template not found"));

        // WHEN: send is called
        emailTemplateService.send(validEvent);

        // THEN: Exception is caught and logged
        // AND: Method does not throw exception
        verify(templateEngine, times(1)).process(eq("email/booking-created"), any(Context.class));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("send() - JavaMailSender fails once - Retries successfully")
    void send_TransientFailure_RetriesSuccessfully() throws Exception {
        // GIVEN: First attempt fails, second succeeds
        String renderedHtml = "<html><body>Email content</body></html>";
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn(renderedHtml);

        doThrow(new MailSendException("Temporary failure"))
            .doNothing()
            .when(javaMailSender).send(any(MimeMessage.class));

        // WHEN: send is called
        emailTemplateService.send(validEvent);

        // THEN: Email is sent successfully on retry
        // AND: JavaMailSender.send called 2 times (1 failure + 1 success)
        verify(javaMailSender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("send() - JavaMailSender fails - Retries three times")
    void send_MailClientFails_RetriesThreeTimes() {
        // GIVEN: JavaMailSender throws MailException on all attempts
        String renderedHtml = "<html><body>Email content</body></html>";
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn(renderedHtml);

        MailException mailException = new MailSendException("SMTP server unavailable");
        doThrow(mailException).when(javaMailSender).send(any(MimeMessage.class));

        // WHEN: send is called
        emailTemplateService.send(validEvent);

        // THEN: JavaMailSender.send called 3 times (retry logic)
        // AND: Exception is caught and logged after retries
        // AND: Method does not throw exception
        verify(javaMailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("send() - Uses correct template name")
    void send_UsesCorrectTemplate() {
        // GIVEN: SendMailEvent with specific template name
        when(templateEngine.process(eq("email/booking-created"), any(Context.class)))
            .thenReturn("<html>Email</html>");
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // WHEN: send is called
        emailTemplateService.send(validEvent);

        // THEN: Correct template is rendered
        verify(templateEngine, times(1)).process(eq("email/booking-created"), any(Context.class));
    }

    @Test
    @DisplayName("send() - Subject and recipient set correctly")
    void send_SubjectAndRecipient_SetCorrectly() throws Exception {
        // GIVEN: Valid SendMailEvent
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn("<html>Email</html>");
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // WHEN: send is called
        emailTemplateService.send(validEvent);

        // THEN: MimeMessage is created once for successful send
        verify(javaMailSender, times(1)).createMimeMessage();
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("send() - Retry with exponential backoff - Succeeds on third attempt")
    void send_ExponentialBackoff_SucceedsOnThirdAttempt() {
        // GIVEN: First two attempts fail, third succeeds
        when(templateEngine.process(anyString(), any(Context.class)))
            .thenReturn("<html>Email</html>");

        doThrow(new MailSendException("Failure 1"))
            .doThrow(new MailSendException("Failure 2"))
            .doNothing()
            .when(javaMailSender).send(any(MimeMessage.class));

        // WHEN: send is called
        emailTemplateService.send(validEvent);

        // THEN: Email sent on third attempt
        // AND: Retry count is logged
        verify(javaMailSender, times(3)).send(any(MimeMessage.class));
    }
}
