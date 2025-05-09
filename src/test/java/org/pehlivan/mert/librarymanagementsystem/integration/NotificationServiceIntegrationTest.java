package org.pehlivan.mert.librarymanagementsystem.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRegistrationNotification;
import org.pehlivan.mert.librarymanagementsystem.service.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private Acknowledgment acknowledgment;

    private UserRegistrationNotification userRegistrationNotification;

    @BeforeEach
    void setUp() {
        userRegistrationNotification = UserRegistrationNotification.builder()
                .email("test@example.com")
                .username("testuser")
                .build();
    }

    @Test
    void handleUserRegistration_Success() {
        // When & Then
        assertDoesNotThrow(() -> notificationService.handleUserRegistration(userRegistrationNotification, acknowledgment));
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void handleUserRegistration_NullEmail() {
        // Given
        userRegistrationNotification.setEmail(null);

        // When & Then
        assertDoesNotThrow(() -> notificationService.handleUserRegistration(userRegistrationNotification, acknowledgment));
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void handleUserRegistration_NullUsername() {
        // Given
        userRegistrationNotification.setUsername(null);

        // When & Then
        assertDoesNotThrow(() -> notificationService.handleUserRegistration(userRegistrationNotification, acknowledgment));
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void handleUserRegistration_EmptyEmail() {
        // Given
        userRegistrationNotification.setEmail("");

        // When & Then
        assertDoesNotThrow(() -> notificationService.handleUserRegistration(userRegistrationNotification, acknowledgment));
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void handleUserRegistration_EmptyUsername() {
        // Given
        userRegistrationNotification.setUsername("");

        // When & Then
        assertDoesNotThrow(() -> notificationService.handleUserRegistration(userRegistrationNotification, acknowledgment));
        verify(acknowledgment, times(1)).acknowledge();
    }
} 