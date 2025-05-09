package org.pehlivan.mert.librarymanagementsystem.service.email;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter welcomeEmailsSentCounter;

    @Mock
    private Counter loanNotificationsSentCounter;

    @Mock
    private Counter overdueNotificationsSentCounter;

    @Mock
    private Counter emailErrorsCounter;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_BOOK_TITLE = "Test Book";
    private static final String TEST_FROM_EMAIL = "library@example.com";

    @BeforeEach
    void setUp() {
        // Setup meter registry mocks
        when(meterRegistry.counter("library.emails.welcome.sent")).thenReturn(welcomeEmailsSentCounter);
        when(meterRegistry.counter("library.emails.loan.sent")).thenReturn(loanNotificationsSentCounter);
        when(meterRegistry.counter("library.emails.overdue.sent")).thenReturn(overdueNotificationsSentCounter);
        when(meterRegistry.counter("library.emails.errors")).thenReturn(emailErrorsCounter);

        // Setup mail sender mock
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Set from email
        ReflectionTestUtils.setField(emailService, "fromEmail", TEST_FROM_EMAIL);

        // Initialize counters
        emailService.init();
    }

    @Test
    void sendWelcomeEmail_Success() {
        // Arrange
        when(templateEngine.process(eq("welcome-notification"), any(Context.class)))
                .thenReturn("<html>Welcome email content</html>");

        // Act
        emailService.sendWelcomeEmail(TEST_EMAIL, TEST_USERNAME);

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
        verify(welcomeEmailsSentCounter).increment();
        verify(emailErrorsCounter, never()).increment();
    }

    @Test
    void sendWelcomeEmail_ThrowsException() {
        // Arrange
        when(templateEngine.process(eq("welcome-notification"), any(Context.class)))
                .thenReturn("<html>Welcome email content</html>");
        doThrow(new RuntimeException("Failed to send email"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendWelcomeEmail(TEST_EMAIL, TEST_USERNAME));
        
        assertEquals("Failed to send welcome email", exception.getMessage());
        verify(welcomeEmailsSentCounter, never()).increment();
        verify(emailErrorsCounter).increment();
    }

    @Test
    void sendLoanNotification_Success() {
        // Arrange
        LocalDate borrowedDate = LocalDate.now();
        LocalDate dueDate = borrowedDate.plusDays(14);
        when(templateEngine.process(eq("loan-notification"), any(Context.class)))
                .thenReturn("<html>Loan notification content</html>");

        // Act
        emailService.sendLoanNotification(TEST_EMAIL, TEST_USERNAME, TEST_BOOK_TITLE, borrowedDate, dueDate);

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
        verify(loanNotificationsSentCounter).increment();
        verify(emailErrorsCounter, never()).increment();
    }

    @Test
    void sendLoanNotification_ThrowsException() {
        // Arrange
        LocalDate borrowedDate = LocalDate.now();
        LocalDate dueDate = borrowedDate.plusDays(14);
        when(templateEngine.process(eq("loan-notification"), any(Context.class)))
                .thenReturn("<html>Loan notification content</html>");
        doThrow(new RuntimeException("Failed to send email"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendLoanNotification(TEST_EMAIL, TEST_USERNAME, TEST_BOOK_TITLE, borrowedDate, dueDate));
        
        assertEquals("Failed to send loan notification", exception.getMessage());
        verify(loanNotificationsSentCounter, never()).increment();
        verify(emailErrorsCounter).increment();
    }

    @Test
    void sendOverdueNotification_Success() {
        // Arrange
        List<Map<String, Object>> overdueBooks = Arrays.asList(
            createOverdueBook("Book 1", LocalDate.now().minusDays(5)),
            createOverdueBook("Book 2", LocalDate.now().minusDays(3))
        );
        when(templateEngine.process(eq("overdue-notification"), any(Context.class)))
                .thenReturn("<html>Overdue notification content</html>");

        // Act
        emailService.sendOverdueNotification(TEST_EMAIL, TEST_USERNAME, overdueBooks);

        // Assert
        verify(mailSender).send(any(MimeMessage.class));
        verify(overdueNotificationsSentCounter).increment();
        verify(emailErrorsCounter, never()).increment();
    }

    @Test
    void sendOverdueNotification_ThrowsException() {
        // Arrange
        List<Map<String, Object>> overdueBooks = Arrays.asList(
            createOverdueBook("Book 1", LocalDate.now().minusDays(5))
        );
        when(templateEngine.process(eq("overdue-notification"), any(Context.class)))
                .thenReturn("<html>Overdue notification content</html>");
        doThrow(new RuntimeException("Failed to send email"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendOverdueNotification(TEST_EMAIL, TEST_USERNAME, overdueBooks));
        
        assertEquals("Failed to send overdue notification", exception.getMessage());
        verify(overdueNotificationsSentCounter, never()).increment();
        verify(emailErrorsCounter).increment();
    }

    @Test
    void sendEmail_Success() {
        // Act
        emailService.sendEmail(TEST_EMAIL, "Test Subject", "Test Content");

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_ThrowsException() {
        // Arrange
        doThrow(new RuntimeException("Failed to send email"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendEmail(TEST_EMAIL, "Test Subject", "Test Content"));
        
        assertEquals("Failed to send email", exception.getMessage());
        verify(emailErrorsCounter).increment();
    }

    @Test
    void sendOverdueNotification_Simple_Success() {
        // Act
        emailService.sendOverdueNotification(TEST_EMAIL, TEST_BOOK_TITLE);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOverdueNotification_Simple_ThrowsException() {
        // Arrange
        doThrow(new RuntimeException("Failed to send email"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            emailService.sendOverdueNotification(TEST_EMAIL, TEST_BOOK_TITLE));
        
        assertEquals("Failed to send overdue notification", exception.getMessage());
        verify(emailErrorsCounter).increment();
    }

    private Map<String, Object> createOverdueBook(String title, LocalDate dueDate) {
        Map<String, Object> book = new HashMap<>();
        book.put("title", title);
        book.put("dueDate", dueDate);
        return book;
    }
} 