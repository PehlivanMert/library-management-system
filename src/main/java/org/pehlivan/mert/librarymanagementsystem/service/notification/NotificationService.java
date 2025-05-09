package org.pehlivan.mert.librarymanagementsystem.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRegistrationNotification;
import org.pehlivan.mert.librarymanagementsystem.service.email.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final MeterRegistry meterRegistry;

    private Counter notificationsReceivedCounter;
    private Counter notificationsProcessedCounter;
    private Counter notificationErrorsCounter;

    @PostConstruct
    public void init() {
        notificationsReceivedCounter = meterRegistry.counter("library.notifications.received");
        notificationsProcessedCounter = meterRegistry.counter("library.notifications.processed");
        notificationErrorsCounter = meterRegistry.counter("library.notifications.errors");
    }

    @KafkaListener(
        topics = "user-registration",
        groupId = "notification-group",
        id = "notification-consumer-1",
        containerFactory = "kafkaListenerContainerFactory",
        autoStartup = "true"
    )
    public void handleUserRegistration(UserRegistrationNotification userData, Acknowledgment ack) {
        try {
            log.info("Received user registration notification: {}", userData);
            notificationsReceivedCounter.increment();
            
            if (userData == null || userData.getEmail() == null || userData.getUsername() == null) {
                log.error("Invalid user data received: {}", userData);
                notificationErrorsCounter.increment();
                ack.acknowledge();
                return;
            }
            
            log.info("Processing user registration for: {}", userData.getUsername());
            
            emailService.sendWelcomeEmail(userData.getEmail(), userData.getUsername());
            
            log.info("Successfully processed user registration for: {}", userData.getUsername());
            notificationsProcessedCounter.increment();
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing user registration notification: {}", e.getMessage(), e);
            notificationErrorsCounter.increment();
            ack.acknowledge();
        }
    }
} 