# E-posta Sistemi / Email System

## Hızlı Geçiş / Quick Navigation
- [Genel Bakış / Overview](#genel-bakış--overview)
- [E-posta Tipleri / Email Types](#e-posta-tipleri--email-types)
- [Yapılandırma / Configuration](#yapılandırma--configuration)
- [Metrikler / Metrics](#metrikler--metrics)
- [E-posta Gönderimi / Email Sending](#e-posta-gönderimi--email-sending)
- [Best Practices](#best-practices)
- [Hata Yönetimi / Error Management](#hata-yönetimi--error-management)
- [Örnek Kullanım / Example Usage](#örnek-kullanım--example-usage)
- [Gelecek Geliştirmeler / Future Improvements](#gelecek-geliştirmeler--future-improvements)

## Genel Bakış / Overview

### Türkçe
Bu projede e-posta gönderimi için Spring Boot'un `JavaMailSender` altyapısı ve Thymeleaf template engine kullanılmaktadır. Sistem, metrik toplama ve loglama özellikleriyle birlikte gelir.

#### Neden E-posta Kullanıyoruz?
1. **Bildirimler**: Kullanıcılara önemli olaylar hakkında bilgi vermek
2. **Doğrulama**: E-posta doğrulama işlemleri
3. **Hatırlatmalar**: Ödünç alınan kitapların iade tarihlerini hatırlatmak
4. **Bilgilendirme**: Sistem güncellemeleri ve duyurular

### English
This project uses Spring Boot's `JavaMailSender` infrastructure and Thymeleaf template engine for email sending. The system comes with metric collection and logging features.

#### Why Do We Use Email?
1. **Notifications**: Informing users about important events
2. **Verification**: Email verification processes
3. **Reminders**: Reminding users about book return dates
4. **Information**: System updates and announcements

## E-posta Tipleri / Email Types

### Türkçe
1. **Hoş Geldiniz E-postası**
   - Yeni kayıt olan kullanıcılara gönderilir
   - Kullanıcı adı ve hoş geldin mesajı içerir
   - `welcome-notification` şablonu kullanılır

2. **Kitap Ödünç Alma Bildirimi**
   - Kitap ödünç alındığında gönderilir
   - Kitap adı, ödünç alma tarihi ve iade tarihi bilgilerini içerir
   - `loan-notification` şablonu kullanılır

3. **Gecikmiş Kitap Bildirimi**
   - İade tarihi geçmiş kitaplar için gönderilir
   - Gecikmiş kitapların listesini içerir
   - `overdue-notification` şablonu kullanılır

### English
1. **Welcome Email**
   - Sent to newly registered users
   - Contains username and welcome message
   - Uses `welcome-notification` template

2. **Book Loan Notification**
   - Sent when a book is borrowed
   - Contains book title, borrow date, and return date
   - Uses `loan-notification` template

3. **Overdue Book Notification**
   - Sent for books past their return date
   - Contains list of overdue books
   - Uses `overdue-notification` template

## Yapılandırma / Configuration

### Türkçe
#### 1. SMTP Yapılandırması
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 2. E-posta Şablonları
Tüm e-posta şablonları `src/main/resources/templates/` dizininde bulunur:
- `welcome-notification.html`: Hoş geldiniz e-postası
- `loan-notification.html`: Ödünç alma bildirimi
- `overdue-notification.html`: Gecikme bildirimi

### English
#### 1. SMTP Configuration
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 2. Email Templates
All email templates are located in `src/main/resources/templates/`:
- `welcome-notification.html`: Welcome email
- `loan-notification.html`: Loan notification
- `overdue-notification.html`: Overdue notification

## Metrikler / Metrics

### Türkçe
Sistem aşağıdaki metrikleri toplar:
- `library.emails.welcome.sent`: Gönderilen hoş geldin e-postaları sayısı
- `library.emails.loan.sent`: Gönderilen ödünç alma bildirimleri sayısı
- `library.emails.overdue.sent`: Gönderilen gecikme bildirimleri sayısı
- `library.emails.errors`: E-posta gönderim hataları sayısı

### English
The system collects the following metrics:
- `library.emails.welcome.sent`: Number of welcome emails sent
- `library.emails.loan.sent`: Number of loan notifications sent
- `library.emails.overdue.sent`: Number of overdue notifications sent
- `library.emails.errors`: Number of email sending errors

## E-posta Gönderimi / Email Sending

### Türkçe
#### 1. Hoş Geldiniz E-postası
```java
public void sendWelcomeEmail(String to, String username) {
    try {
        Context context = new Context();
        context.setVariable("userName", username);
        
        String htmlContent = templateEngine.process("welcome-notification", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Kütüphane Yönetim Sistemine Hoş Geldiniz");
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
        welcomeEmailsSentCounter.increment();
    } catch (MessagingException e) {
        emailErrorsCounter.increment();
        throw new RuntimeException("Failed to send welcome email", e);
    }
}
```

### English
#### 1. Welcome Email
```java
public void sendWelcomeEmail(String to, String username) {
    try {
        Context context = new Context();
        context.setVariable("userName", username);
        
        String htmlContent = templateEngine.process("welcome-notification", context);
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Welcome to Library Management System");
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
        welcomeEmailsSentCounter.increment();
    } catch (MessagingException e) {
        emailErrorsCounter.increment();
        throw new RuntimeException("Failed to send welcome email", e);
    }
}
```

## Best Practices

### Türkçe
1. **Loglama**
   - Her e-posta gönderimi loglanır
   - Başarılı/başarısız durumlar ayrı ayrı loglanır
   - Hata durumları detaylı loglanır

2. **Performans**
   - HTML şablonları önbelleğe alınır
   - E-posta gönderimi asenkron yapılabilir
   - Toplu e-posta gönderimleri optimize edilir

3. **Güvenlik**
   - SMTP kimlik doğrulama kullanılır
   - E-posta içeriği HTML escape edilir
   - Hassas bilgiler şifrelenir

### English
1. **Logging**
   - Every email sending is logged
   - Success/failure states are logged separately
   - Error states are logged in detail

2. **Performance**
   - HTML templates are cached
   - Email sending can be asynchronous
   - Bulk email sending is optimized

3. **Security**
   - SMTP authentication is used
   - Email content is HTML escaped
   - Sensitive information is encrypted

## Hata Yönetimi / Error Management

### Türkçe
#### 1. Hata Türleri
- SMTP bağlantı hataları
- Şablon işleme hataları
- Mesaj oluşturma hataları

#### 2. Hata İşleme
- Tüm hatalar loglanır
- Hata metrikleri toplanır
- Kullanıcıya uygun hata mesajları döndürülür

### English
#### 1. Error Types
- SMTP connection errors
- Template processing errors
- Message creation errors

#### 2. Error Handling
- All errors are logged
- Error metrics are collected
- Appropriate error messages are returned to users

## Örnek Kullanım / Example Usage

### Türkçe
```java
// Kullanıcı kaydı
public void sendWelcomeEmail(User user) {
    Map<String, Object> model = new HashMap<>();
    model.put("name", user.getFullName());
    model.put("username", user.getUsername());
    
    sendWelcomeEmail(
        user.getEmail(),
        user.getUsername()
    );
}

// Kitap iade hatırlatması
public void sendReturnReminder(Loan loan) {
    Map<String, Object> model = new HashMap<>();
    model.put("bookTitle", loan.getBook().getTitle());
    model.put("dueDate", loan.getDueDate());
    
    sendLoanNotification(
        loan.getUser().getEmail(),
        loan.getUser().getUsername(),
        loan.getBook().getTitle(),
        loan.getBorrowedDate(),
        loan.getDueDate()
    );
}
```

### English
```java
// User registration
public void sendWelcomeEmail(User user) {
    Map<String, Object> model = new HashMap<>();
    model.put("name", user.getFullName());
    model.put("username", user.getUsername());
    
    sendWelcomeEmail(
        user.getEmail(),
        user.getUsername()
    );
}

// Book return reminder
public void sendReturnReminder(Loan loan) {
    Map<String, Object> model = new HashMap<>();
    model.put("bookTitle", loan.getBook().getTitle());
    model.put("dueDate", loan.getDueDate());
    
    sendLoanNotification(
        loan.getUser().getEmail(),
        loan.getUser().getUsername(),
        loan.getBook().getTitle(),
        loan.getBorrowedDate(),
        loan.getDueDate()
    );
}
```

## Gelecek Geliştirmeler / Future Improvements

### Türkçe
1. **Yeni E-posta Tipleri**:
   - Şifre sıfırlama
   - Hesap güncelleme
   - Özel duyurular

2. **Gelişmiş Özellikler**:
   - E-posta takibi
   - A/B testing
   - Kişiselleştirilmiş içerik

3. **Entegrasyonlar**:
   - E-posta servis sağlayıcıları
   - Analytics
   - Spam koruması

### English
1. **New Email Types**:
   - Password reset
   - Account updates
   - Custom announcements

2. **Advanced Features**:
   - Email tracking
   - A/B testing
   - Personalized content

3. **Integrations**:
   - Email service providers
   - Analytics
   - Spam protection 