package org.pehlivan.mert.librarymanagementsystem.service.email;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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
        welcomeEmailsSentCounter = Counter.builder("library.emails.welcome.sent")
                .description("Number of welcome emails sent")
                .register(meterRegistry);

        loanNotificationsSentCounter = Counter.builder("library.emails.loan.sent")
                .description("Number of loan notification emails sent")
                .register(meterRegistry);

        overdueNotificationsSentCounter = Counter.builder("library.emails.overdue.sent")
                .description("Number of overdue notification emails sent")
                .register(meterRegistry);

        emailErrorsCounter = Counter.builder("library.emails.errors")
                .description("Number of email sending errors")
                .register(meterRegistry);
    }

    public void sendWelcomeEmail(String to, String username) {
        try {
            log.info("Preparing welcome email for user: {}", username);
            
            Context context = new Context();
            context.setVariable("userName", username);
            
            String htmlContent = templateEngine.process("welcome-notification", context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Kütüphane Yönetim Sistemine Hoş Geldiniz");
            helper.setText(htmlContent, true);
            
            log.info("Sending welcome email to: {}", to);
            mailSender.send(message);
            welcomeEmailsSentCounter.increment();
            log.info("Welcome email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
            emailErrorsCounter.increment();
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    public void sendLoanNotification(String to, String userName, String bookTitle, 
                                   LocalDate borrowedDate, LocalDate dueDate) {
        try {
            log.info("Preparing loan notification email for user: {}", userName);
            
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("bookTitle", bookTitle);
            context.setVariable("borrowedDate", borrowedDate);
            context.setVariable("dueDate", dueDate);

            String htmlContent = templateEngine.process("loan-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Kitap Ödünç Alma Bildirimi");
            helper.setText(htmlContent, true);

            log.info("Sending loan notification email to: {}", to);
            mailSender.send(message);
            loanNotificationsSentCounter.increment();
            log.info("Loan notification email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send loan notification email to: {}", to, e);
            emailErrorsCounter.increment();
            throw new RuntimeException("Failed to send loan notification email", e);
        }
    }

    public void sendOverdueNotification(String to, String userName, List<Map<String, Object>> overdueBooks) {
        try {
            log.info("Preparing overdue notification email for user: {}", userName);
            
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("books", overdueBooks);

            String htmlContent = templateEngine.process("overdue-notification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Gecikmiş Kitap Bildirimi");
            helper.setText(htmlContent, true);

            log.info("Sending overdue notification email to: {}", to);
            mailSender.send(message);
            overdueNotificationsSentCounter.increment();
            log.info("Overdue notification email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send overdue notification email to: {}", to, e);
            emailErrorsCounter.increment();
            throw new RuntimeException("Failed to send overdue notification email", e);
        }
    }

    public void sendEmail(String to, String subject, String text) {
        log.info("Sending email to: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage());
            throw e;
        }
    }

    public void sendOverdueNotification(String to, String bookTitle) {
        log.info("Sending overdue notification for book: {} to: {}", bookTitle, to);
        String subject = "Overdue Book Notification";
        String text = String.format("The book '%s' is overdue. Please return it as soon as possible.", bookTitle);
        
        try {
            sendEmail(to, subject, text);
        } catch (Exception e) {
            log.error("Error sending overdue notification: {}", e.getMessage());
            throw e;
        }
    }
} 