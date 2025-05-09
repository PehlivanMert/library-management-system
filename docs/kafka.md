# Kafka Entegrasyonu / Kafka Integration

## Hızlı Geçiş / Quick Navigation
- [Genel Bakış / Overview](#genel-bakış--overview)
- [Neden Kafka? / Why Kafka?](#neden-kafka--why-kafka)
- [Yapılandırma / Configuration](#yapılandırma--configuration)
- [Mesaj Gönderimi / Message Sending](#mesaj-gönderimi--message-sending)
- [Mesaj İşleme / Message Processing](#mesaj-işleme--message-processing)
- [Hata Yönetimi / Error Management](#hata-yönetimi--error-management)
- [Monitoring](#monitoring)
- [Best Practices](#best-practices)
- [Örnek Kullanım / Example Usage](#örnek-kullanım--example-usage)
- [Gelecek Geliştirmeler / Future Improvements](#gelecek-geliştirmeler--future-improvements)

## Genel Bakış / Overview

### Türkçe
Bu projede Kafka, kullanıcı kayıt bildirimleri için asenkron mesajlaşma sistemi olarak kullanılmaktadır. Kafka, e-posta gönderimi gibi zaman alan işlemleri ana uygulama akışından ayırarak sistemin performansını artırmaktadır.

#### Entegrasyon Detayları
1. **Servisler Arası İletişim**:
   - `AuthenticationService` ve `UserService`: Kullanıcı kayıt işlemlerini gerçekleştirir
   - `NotificationService`: Kafka mesajlarını dinler ve e-posta gönderimi yapar
   - `EmailService`: E-posta gönderim işlemlerini gerçekleştirir

2. **Veri Akışı**:
   ```
   User Registration -> Kafka -> Notification Service -> Email Service
   ```

3. **Kullanılan Teknolojiler**:
   - Spring Kafka
   - JSON Serialization
   - Thymeleaf Templates
   - JavaMailSender

### English
In this project, Kafka is used as an asynchronous messaging system for user registration notifications. Kafka improves system performance by separating time-consuming operations like email sending from the main application flow.

#### Integration Details
1. **Inter-Service Communication**:
   - `AuthenticationService` and `UserService`: Handle user registration
   - `NotificationService`: Listens to Kafka messages and sends emails
   - `EmailService`: Handles email sending operations

2. **Data Flow**:
   ```
   User Registration -> Kafka -> Notification Service -> Email Service
   ```

3. **Technologies Used**:
   - Spring Kafka
   - JSON Serialization
   - Thymeleaf Templates
   - JavaMailSender

## Neden Kafka? / Why Kafka?

### Türkçe
1. **Asenkron İşlemler**:
   - E-posta gönderimi gibi zaman alan işlemleri ana uygulama akışından ayırıyoruz
   - Kullanıcı kaydı işlemi e-posta gönderimini beklemez
   - Sistem yanıt süresi iyileştirilir

2. **Güvenilirlik**:
   - Mesajların kaybolmamasını sağlıyoruz
   - En az bir kez işlenmesini garanti ediyoruz
   - Hata durumunda yeniden deneme mekanizması

3. **Ölçeklenebilirlik**:
   - Bildirim işlemlerini ayrı servislere taşıyabiliyoruz
   - Yük dengeleme yapabiliyoruz
   - Yatay ölçeklendirme imkanı

4. **Bağımsızlık**:
   - Servisler arası gevşek bağlantı
   - Bağımsız geliştirme ve deployment
   - Daha iyi hata izolasyonu

### English
1. **Asynchronous Operations**:
   - Separates time-consuming operations like email sending from main flow
   - User registration doesn't wait for email sending
   - Improves system response time

2. **Reliability**:
   - Ensures messages are not lost
   - Guarantees at least once processing
   - Retry mechanism for error cases

3. **Scalability**:
   - Can move notification operations to separate services
   - Enables load balancing
   - Horizontal scaling capability

4. **Independence**:
   - Loose coupling between services
   - Independent development and deployment
   - Better error isolation

## Yapılandırma / Configuration

### Türkçe
#### 1. Kafka Konfigürasyonu
```java
@Configuration
public class KafkaConfig {
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userRegistrationTopic() {
        return new NewTopic("user-registration", 1, (short) 1);
    }
}
```

#### 2. Producer Yapılandırması
```java
@Bean
public ProducerFactory<String, UserRegistrationNotification> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    configProps.put(JsonSerializer.TYPE_MAPPINGS, "userRegistration:org.pehlivan.mert.librarymanagementsystem.dto.UserRegistrationNotification");
    return new DefaultKafkaProducerFactory<>(configProps);
}
```

#### 3. Consumer Yapılandırması
```java
@Bean
public ConsumerFactory<String, UserRegistrationNotification> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 20000);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.pehlivan.mert.librarymanagementsystem.dto.UserRegistrationNotification");
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, true);
    return new DefaultKafkaConsumerFactory<>(props);
}
```

### English
#### 1. Kafka Configuration
```java
@Configuration
public class KafkaConfig {
    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userRegistrationTopic() {
        return new NewTopic("user-registration", 1, (short) 1);
    }
}
```

#### 2. Producer Configuration
```java
@Bean
public ProducerFactory<String, UserRegistrationNotification> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
    configProps.put(JsonSerializer.TYPE_MAPPINGS, "userRegistration:org.pehlivan.mert.librarymanagementsystem.dto.UserRegistrationNotification");
    return new DefaultKafkaProducerFactory<>(configProps);
}
```

#### 3. Consumer Configuration
```java
@Bean
public ConsumerFactory<String, UserRegistrationNotification> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);
    props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 20000);
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "org.pehlivan.mert.librarymanagementsystem.dto.UserRegistrationNotification");
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, true);
    return new DefaultKafkaConsumerFactory<>(props);
}
```

## Mesaj Gönderimi / Message Sending

### Türkçe
#### 1. DTO Oluşturma
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationNotification {
    private String email;
    private String username;
}
```

#### 2. Mesaj Gönderme
```java
// AuthenticationService içinde
UserRegistrationNotification notification = new UserRegistrationNotification(
    createdUser.getEmail(),
    createdUser.getUsername()
);

kafkaTemplate.send("user-registration", notification)
    .whenComplete((result, ex) -> {
        if (ex == null) {
            log.info("Message sent successfully");
        } else {
            log.error("Failed to send message", ex);
        }
    });
```

### English
#### 1. DTO Creation
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationNotification {
    private String email;
    private String username;
}
```

#### 2. Message Sending
```java
// Inside AuthenticationService
UserRegistrationNotification notification = new UserRegistrationNotification(
    createdUser.getEmail(),
    createdUser.getUsername()
);

kafkaTemplate.send("user-registration", notification)
    .whenComplete((result, ex) -> {
        if (ex == null) {
            log.info("Message sent successfully");
        } else {
            log.error("Failed to send message", ex);
        }
    });
```

## Mesaj İşleme / Message Processing

### Türkçe
#### 1. Listener Tanımlama
```java
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
        
        if (userData == null || userData.getEmail() == null || userData.getUsername() == null) {
            log.error("Invalid user data received: {}", userData);
            ack.acknowledge();
            return;
        }
        
        log.info("Processing user registration for: {}", userData.getUsername());
        
        emailService.sendWelcomeEmail(userData.getEmail(), userData.getUsername());
        
        log.info("Successfully processed user registration for: {}", userData.getUsername());
        ack.acknowledge();
        
    } catch (Exception e) {
        log.error("Error processing user registration notification: {}", e.getMessage(), e);
        ack.acknowledge();
    }
}
```

### English
#### 1. Listener Definition
```java
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
        
        if (userData == null || userData.getEmail() == null || userData.getUsername() == null) {
            log.error("Invalid user data received: {}", userData);
            ack.acknowledge();
            return;
        }
        
        log.info("Processing user registration for: {}", userData.getUsername());
        
        emailService.sendWelcomeEmail(userData.getEmail(), userData.getUsername());
        
        log.info("Successfully processed user registration for: {}", userData.getUsername());
        ack.acknowledge();
        
    } catch (Exception e) {
        log.error("Error processing user registration notification: {}", e.getMessage(), e);
        ack.acknowledge();
    }
}
```

## Hata Yönetimi / Error Management

### Türkçe
1. **Producer Tarafında**:
   - Retry mekanizması (3 deneme)
   - Hata loglama
   - Kullanıcı kaydı etkilenmez

2. **Consumer Tarafında**:
   - Try-catch blokları
   - Hata loglama
   - Manuel onay (acknowledge)
   - Geçersiz veri kontrolü

### English
1. **On Producer Side**:
   - Retry mechanism (3 attempts)
   - Error logging
   - User registration not affected

2. **On Consumer Side**:
   - Try-catch blocks
   - Error logging
   - Manual acknowledgment
   - Invalid data validation

## Monitoring

### Türkçe
1. **Loglar**:
   - Mesaj gönderimi
   - Mesaj işleme
   - Hata durumları
   - Geçersiz veri durumları

2. **Metrikler**:
   - Başarılı mesaj sayısı
   - Başarısız mesaj sayısı
   - İşlem süreleri
   - Kuyruk boyutları

### English
1. **Logs**:
   - Message sending
   - Message processing
   - Error states
   - Invalid data states

2. **Metrics**:
   - Successful message count
   - Failed message count
   - Processing times
   - Queue sizes

## Best Practices

### Türkçe
1. **Mesaj Formatı**:
   - JSON kullanımı
   - Type mapping
   - Trusted packages

2. **Güvenlik**:
   - Trusted packages sınırlaması
   - Type info headers kaldırma
   - Manuel onay mekanizması

3. **Performans**:
   - Uygun timeout değerleri
   - Heartbeat ayarları
   - Poll timeout ayarları

### English
1. **Message Format**:
   - JSON usage
   - Type mapping
   - Trusted packages

2. **Security**:
   - Trusted packages limitation
   - Type info headers removal
   - Manual acknowledgment mechanism

3. **Performance**:
   - Appropriate timeout values
   - Heartbeat settings
   - Poll timeout settings

## Örnek Kullanım / Example Usage

### Türkçe
1. **Kullanıcı Kaydı**:
   ```java
   // AuthenticationService içinde
   @Transactional
   public UserResponseDto register(UserRequestDto userRequestDto) {
       // Kullanıcı kaydı
       User createdUser = userRepository.save(user);
       
       // Kafka mesajı gönderimi
       UserRegistrationNotification notification = new UserRegistrationNotification(
           createdUser.getEmail(),
           createdUser.getUsername()
       );
       
       kafkaTemplate.send("user-registration", notification);
       
       return modelMapper.map(createdUser, UserResponseDto.class);
   }
   ```

2. **Bildirim İşleme**:
   ```java
   // NotificationService içinde
   @KafkaListener(topics = "user-registration")
   public void handleUserRegistration(UserRegistrationNotification userData) {
       emailService.sendWelcomeEmail(userData.getEmail(), userData.getUsername());
   }
   ```

### English
1. **User Registration**:
   ```java
   // Inside AuthenticationService
   @Transactional
   public UserResponseDto register(UserRequestDto userRequestDto) {
       // User registration
       User createdUser = userRepository.save(user);
       
       // Kafka message sending
       UserRegistrationNotification notification = new UserRegistrationNotification(
           createdUser.getEmail(),
           createdUser.getUsername()
       );
       
       kafkaTemplate.send("user-registration", notification);
       
       return modelMapper.map(createdUser, UserResponseDto.class);
   }
   ```

2. **Notification Processing**:
   ```java
   // Inside NotificationService
   @KafkaListener(topics = "user-registration")
   public void handleUserRegistration(UserRegistrationNotification userData) {
       emailService.sendWelcomeEmail(userData.getEmail(), userData.getUsername());
   }
   ```

## Gelecek Geliştirmeler / Future Improvements

### Türkçe
1. **Yeni Topic'ler**:
   - `loan-notification`: Kitap ödünç alma bildirimleri
   - `overdue-notification`: Gecikmiş kitap bildirimleri
   - `book-availability`: Kitap müsaitlik bildirimleri

2. **Yeni DTO'lar**:
   - `LoanNotification`: Ödünç alma bilgileri
   - `OverdueNotification`: Gecikme bilgileri
   - `BookAvailabilityNotification`: Müsaitlik bilgileri

3. **Yeni Listener'lar**:
   - Ödünç alma bildirimleri için listener
   - Gecikme bildirimleri için listener
   - Müsaitlik bildirimleri için listener

### English
1. **New Topics**:
   - `loan-notification`: Book loan notifications
   - `overdue-notification`: Overdue book notifications
   - `book-availability`: Book availability notifications

2. **New DTOs**:
   - `LoanNotification`: Loan information
   - `OverdueNotification`: Overdue information
   - `BookAvailabilityNotification`: Availability information

3. **New Listeners**:
   - Listener for loan notifications
   - Listener for overdue notifications
   - Listener for availability notifications 