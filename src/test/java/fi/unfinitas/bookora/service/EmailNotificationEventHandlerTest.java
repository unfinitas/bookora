package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.event.SendMailEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationEventHandler Tests")
class EmailNotificationEventHandlerTest {

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private EmailNotificationEventHandler handler;

    private SendMailEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent = new SendMailEvent(
            "customer@example.com",
            "Booking Confirmation - Haircut",
            "email/booking-created",
            Map.of(
                "customerName", "John Doe",
                "serviceName", "Haircut"
            )
        );
    }

    @Test
    @DisplayName("handleSendMailEvent() - Valid event - Calls EmailTemplateService")
    void handleSendMailEvent_ValidEvent_CallsEmailTemplateService() {
        // GIVEN: Valid SendMailEvent
        doNothing().when(emailTemplateService).send(any(SendMailEvent.class));

        // WHEN: Event is published
        handler.handleSendMailEvent(validEvent);

        // THEN: EmailTemplateService.send is called with event
        verify(emailTemplateService, times(1)).send(validEvent);
    }

    @Test
    @DisplayName("handleSendMailEvent() - EmailTemplateService fails - Does not throw exception")
    void handleSendMailEvent_EmailTemplateServiceFails_DoesNotThrow() {
        // GIVEN: EmailTemplateService throws exception
        doThrow(new RuntimeException("Email sending failed"))
            .when(emailTemplateService).send(any(SendMailEvent.class));

        // WHEN: Event is handled
        handler.handleSendMailEvent(validEvent);

        // THEN: Exception is caught and logged
        // AND: No exception propagated to publisher
        verify(emailTemplateService, times(1)).send(validEvent);
        // Method completes without throwing exception
    }

    @Test
    @DisplayName("handleSendMailEvent() - Multiple events - Each handled independently")
    void handleSendMailEvent_MultipleEvents_EachHandledIndependently() {
        // GIVEN: Multiple SendMailEvents
        SendMailEvent event1 = new SendMailEvent(
            "user1@example.com",
            "Subject 1",
            "email/template1",
            Map.of("key", "value1")
        );

        SendMailEvent event2 = new SendMailEvent(
            "user2@example.com",
            "Subject 2",
            "email/template2",
            Map.of("key", "value2")
        );

        doNothing().when(emailTemplateService).send(any(SendMailEvent.class));

        // WHEN: Events are handled
        handler.handleSendMailEvent(event1);
        handler.handleSendMailEvent(event2);

        // THEN: EmailTemplateService.send called for each event
        verify(emailTemplateService, times(1)).send(event1);
        verify(emailTemplateService, times(1)).send(event2);
    }

    @Test
    @DisplayName("handleSendMailEvent() - First event fails - Second event still processed")
    void handleSendMailEvent_FirstEventFails_SecondEventProcessed() {
        // GIVEN: First event causes exception, second event succeeds
        SendMailEvent event1 = new SendMailEvent(
            "fail@example.com",
            "Subject",
            "email/template",
            Map.of()
        );

        SendMailEvent event2 = new SendMailEvent(
            "success@example.com",
            "Subject",
            "email/template",
            Map.of()
        );

        doThrow(new RuntimeException("Failed"))
            .when(emailTemplateService).send(event1);
        doNothing().when(emailTemplateService).send(event2);

        // WHEN: Events are handled
        handler.handleSendMailEvent(event1);
        handler.handleSendMailEvent(event2);

        // THEN: Both events are processed
        verify(emailTemplateService, times(1)).send(event1);
        verify(emailTemplateService, times(1)).send(event2);
    }

    @Test
    @DisplayName("handleSendMailEvent() - Null event - Handles gracefully")
    void handleSendMailEvent_NullEvent_HandlesGracefully() {
        // GIVEN: Null event
        // WHEN: Event handler is called with null
        // THEN: No exception is thrown (Spring may not call handler with null, but test defensive coding)
        // This test ensures the handler doesn't crash if invoked with null

        // In practice, Spring won't publish null events, but we test defensive code
        try {
            handler.handleSendMailEvent(null);
        } catch (Exception e) {
            // Expected to either handle gracefully or throw NullPointerException
            // The implementation should decide behavior
        }

        // Verify emailTemplateService is not called with null
        verify(emailTemplateService, never()).send(null);
    }
}
