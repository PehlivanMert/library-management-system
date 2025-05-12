package org.pehlivan.mert.librarymanagementsystem.integration;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.service.email.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "management.health.mail.enabled=false",
    "spring.mail.enabled=false"
})
@ActiveProfiles("test")
@Transactional
public class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private MimeMessage mimeMessage;

    private String testEmail;
    private String testUsername;
    private String testBookTitle;
    private LocalDate testBorrowedDate;
    private LocalDate testDueDate;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testUsername = "testuser";
        testBookTitle = "Test Book";
        testBorrowedDate = LocalDate.now();
        testDueDate = LocalDate.now().plusDays(14);
        
        // Mock mail sender methods
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendWelcomeEmail_Success() {
        // When & Then
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(testEmail, testUsername));
    }

    @Test
    void sendLoanNotification_Success() {
        // When & Then
        assertDoesNotThrow(() -> emailService.sendLoanNotification(
                testEmail,
                testUsername,
                testBookTitle,
                testBorrowedDate,
                testDueDate
        ));
    }

    @Test
    void sendOverdueNotification_Success() {
        // Given
        List<Map<String, Object>> overdueBooks = new ArrayList<>();
        Map<String, Object> book = new HashMap<>();
        book.put("title", testBookTitle);
        book.put("dueDate", testDueDate);
        book.put("borrowedDate", testBorrowedDate);
        book.put("overdueDays", 5);
        book.put("penaltyAmount", "50.00 TL");
        overdueBooks.add(book);

        // When & Then
        assertDoesNotThrow(() -> emailService.sendOverdueNotification(
                testEmail,
                testUsername,
                overdueBooks
        ));
    }

    @Test
    void sendEmail_Success() {
        // Given
        String subject = "Test Subject";
        String content = "Test Content";

        // When & Then
        assertDoesNotThrow(() -> emailService.sendEmail(testEmail, subject, content));
    }

    @Test
    void sendOverdueNotification_SingleBook_Success() {
        // When & Then
        assertDoesNotThrow(() -> emailService.sendOverdueNotification(
                testEmail,
                testBookTitle
        ));
    }
} 