package org.pehlivan.mert.librarymanagementsystem.service.notification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRegistrationNotification;
import org.pehlivan.mert.librarymanagementsystem.service.email.EmailService;
import org.springframework.kafka.support.Acknowledgment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter notificationsReceivedCounter;

    @Mock
    private Counter notificationsProcessedCounter;

    @Mock
    private Counter notificationErrorsCounter;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private NotificationService notificationService;

    private UserRegistrationNotification validNotification;
    private UserRegistrationNotification invalidNotification;

    @BeforeEach
    void setUp() {
        // Setup valid notification
        validNotification = UserRegistrationNotification.builder()
                .email("test@example.com")
                .username("testuser")
                .build();

        // Setup invalid notification
        invalidNotification = UserRegistrationNotification.builder()
                .email(null)
                .username(null)
                .build();

        // Setup meter registry mocks
        when(meterRegistry.counter("library.notifications.received")).thenReturn(notificationsReceivedCounter);
        when(meterRegistry.counter("library.notifications.processed")).thenReturn(notificationsProcessedCounter);
        when(meterRegistry.counter("library.notifications.errors")).thenReturn(notificationErrorsCounter);

        // Initialize counters
        notificationService.init();
    }

    @Test
    void handleUserRegistration_Success() {
        // Act
        notificationService.handleUserRegistration(validNotification, acknowledgment);

        // Assert
        verify(emailService).sendWelcomeEmail(validNotification.getEmail(), validNotification.getUsername());
        verify(notificationsReceivedCounter).increment();
        verify(notificationsProcessedCounter).increment();
        verify(notificationErrorsCounter, never()).increment();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleUserRegistration_InvalidData() {
        // Act
        notificationService.handleUserRegistration(invalidNotification, acknowledgment);

        // Assert
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        verify(notificationsReceivedCounter).increment();
        verify(notificationsProcessedCounter, never()).increment();
        verify(notificationErrorsCounter).increment();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleUserRegistration_NullNotification() {
        // Act
        notificationService.handleUserRegistration(null, acknowledgment);

        // Assert
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        verify(notificationsReceivedCounter).increment();
        verify(notificationsProcessedCounter, never()).increment();
        verify(notificationErrorsCounter).increment();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleUserRegistration_EmailServiceThrowsException() {
        // Arrange
        doThrow(new RuntimeException("Email service error"))
                .when(emailService)
                .sendWelcomeEmail(anyString(), anyString());

        // Act
        notificationService.handleUserRegistration(validNotification, acknowledgment);

        // Assert
        verify(emailService).sendWelcomeEmail(validNotification.getEmail(), validNotification.getUsername());
        verify(notificationsReceivedCounter).increment();
        verify(notificationsProcessedCounter, never()).increment();
        verify(notificationErrorsCounter).increment();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleUserRegistration_NullEmail() {
        // Arrange
        UserRegistrationNotification notificationWithNullEmail = UserRegistrationNotification.builder()
                .email(null)
                .username("testuser")
                .build();

        // Act
        notificationService.handleUserRegistration(notificationWithNullEmail, acknowledgment);

        // Assert
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        verify(notificationsReceivedCounter).increment();
        verify(notificationsProcessedCounter, never()).increment();
        verify(notificationErrorsCounter).increment();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleUserRegistration_NullUsername() {
        // Arrange
        UserRegistrationNotification notificationWithNullUsername = UserRegistrationNotification.builder()
                .email("test@example.com")
                .username(null)
                .build();

        // Act
        notificationService.handleUserRegistration(notificationWithNullUsername, acknowledgment);

        // Assert
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        verify(notificationsReceivedCounter).increment();
        verify(notificationsProcessedCounter, never()).increment();
        verify(notificationErrorsCounter).increment();
        verify(acknowledgment).acknowledge();
    }
} 