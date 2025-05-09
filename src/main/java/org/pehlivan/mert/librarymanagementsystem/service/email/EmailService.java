package org.pehlivan.mert.librarymanagementsystem.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;
    private final JavaMailSender mailSender;
    private final MeterRegistry meterRegistry;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private Counter welcomeEmailsSentCounter;
    private Counter loanNotificationsSentCounter;
    private Counter overdueNotificationsSentCounter;
    private Counter emailErrorsCounter;

    @PostConstruct
    public void init() {
        welcomeEmailsSentCounter = meterRegistry.counter("library.emails.welcome.sent");
        loanNotificationsSentCounter = meterRegistry.counter("library.emails.loan.sent");
        overdueNotificationsSentCounter = meterRegistry.counter("library.emails.overdue.sent");
        emailErrorsCounter = meterRegistry.counter("library.emails.errors");
    }

    public void sendWelcomeEmail(String to, String username) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            String emailContent = templateEngine.process("welcome-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to Library Management System");
            helper.setText(emailContent, true);

            mailSender.send(message);
            welcomeEmailsSentCounter.increment();
            log.info("Welcome email sent to: {}", to);
        } catch (Exception e) {
            emailErrorsCounter.increment();
            log.error("Failed to send welcome email to: {}", to, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    public void sendLoanNotification(String to, String username, String bookTitle, LocalDate borrowedDate, LocalDate dueDate) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("bookTitle", bookTitle);
            context.setVariable("borrowedDate", borrowedDate);
            context.setVariable("dueDate", dueDate);
            String emailContent = templateEngine.process("loan-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Book Loan Notification");
            helper.setText(emailContent, true);

            mailSender.send(message);
            loanNotificationsSentCounter.increment();
            log.info("Loan notification sent to: {}", to);
        } catch (Exception e) {
            emailErrorsCounter.increment();
            log.error("Failed to send loan notification to: {}", to, e);
            throw new RuntimeException("Failed to send loan notification", e);
        }
    }

    public void sendOverdueNotification(String to, String username, List<Map<String, Object>> overdueBooks) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("overdueBooks", overdueBooks);
            String emailContent = templateEngine.process("overdue-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Overdue Books Notification");
            helper.setText(emailContent, true);

            mailSender.send(message);
            overdueNotificationsSentCounter.increment();
            log.info("Overdue notification sent to: {}", to);
        } catch (Exception e) {
            emailErrorsCounter.increment();
            log.error("Failed to send overdue notification to: {}", to, e);
            throw new RuntimeException("Failed to send overdue notification", e);
        }
    }

    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            emailErrorsCounter.increment();
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendOverdueNotification(String to, String bookTitle) {
        String subject = "Overdue Book Notification";
        String content = String.format("The book '%s' is overdue. Please return it to the library as soon as possible.", bookTitle);
        try {
            sendEmail(to, subject, content);
        } catch (Exception e) {
            log.error("Failed to send overdue notification to: {}", to, e);
            throw new RuntimeException("Failed to send overdue notification", e);
        }
    }
} 