# Reactive Programming ve Kitap Müsaitlik Takibi / Reactive Programming and Book Availability Tracking

## Hızlı Geçiş / Quick Navigation
- [Genel Bakış / Overview](#genel-bakış--overview)
- [Reactive Programming Nedir? / What is Reactive Programming?](#reactive-programming-nedir--what-is-reactive-programming)
- [Spring WebFlux / Spring WebFlux](#spring-webflux--spring-webflux)
- [Kitap Müsaitlik Takibi / Book Availability Tracking](#kitap-müsaitlik-takibi--book-availability-tracking)
- [Teknik Detaylar / Technical Details](#teknik-detaylar--technical-details)
- [Best Practices](#best-practices)
- [Örnek Kullanım / Example Usage](#örnek-kullanım--example-usage)

## Genel Bakış / Overview

### Türkçe
Bu dokümantasyon, kütüphane yönetim sistemindeki reactive programlama implementasyonunu ve kitap müsaitlik takip sistemini açıklar. Sistem, Spring WebFlux kullanarak gerçek zamanlı kitap müsaitlik durumu değişikliklerini takip eder.

### English
This documentation explains the reactive programming implementation and book availability tracking system in the library management system. The system uses Spring WebFlux to track real-time book availability status changes.

## Reactive Programming Nedir? / What is Reactive Programming?

### Türkçe
Reactive Programming, veri akışlarını ve değişikliklerin yayılmasını ele alan bir programlama paradigmasıdır. Temel özellikleri:

- **Asenkron**: İşlemler eşzamanlı olmayan şekilde gerçekleşir
- **Non-blocking**: İşlemler birbirini bloklamaz
- **Event-driven**: Olay tabanlı çalışır
- **Backpressure**: Veri akışını kontrol eder

### English
Reactive Programming is a programming paradigm that deals with data streams and the propagation of change. Key features:

- **Asynchronous**: Operations occur in a non-synchronous manner
- **Non-blocking**: Operations don't block each other
- **Event-driven**: Works based on events
- **Backpressure**: Controls data flow

## Spring WebFlux / Spring WebFlux

### Türkçe
Spring WebFlux, Spring Framework'ün reactive web uygulamaları için sağladığı bir modüldür. Özellikleri:

- Project Reactor tabanlı
- Non-blocking I/O
- Reactive Streams desteği
- Asenkron endpoint'ler
- Event loop modeli

### English
Spring WebFlux is a module provided by Spring Framework for reactive web applications. Features:

- Project Reactor based
- Non-blocking I/O
- Reactive Streams support
- Asynchronous endpoints
- Event loop model

## Kitap Müsaitlik Takibi / Book Availability Tracking

### Türkçe
Sistem, kitapların müsaitlik durumundaki değişiklikleri gerçek zamanlı olarak takip eder. Bu özellik şu durumlarda kullanılır:

- Kitap ödünç alındığında
- Kitap iade edildiğinde
- Kitap stok durumu güncellendiğinde

#### Bileşenler / Components

1. **BookAvailabilityEvent**:
```java
public class BookAvailabilityEvent {
    private Long bookId;
    private String bookTitle;
    private boolean available;
    private LocalDateTime timestamp;
    private String eventType; // BORROWED, RETURNED, UPDATED
}
```

2. **BookAvailabilityService**:
```java
@Service
public class BookAvailabilityService {
    private final Sinks.Many<BookAvailabilityEvent> bookAvailabilitySink = 
        Sinks.many().multicast().onBackpressureBuffer();

    public void notifyAvailabilityChange(Book book, String eventType) {
        BookAvailabilityEvent event = BookAvailabilityEvent.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .available(book.getAvailableCount() > 0)
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .build();

        bookAvailabilitySink.tryEmitNext(event);
    }

    public Flux<BookAvailabilityEvent> getAvailabilityStream() {
        return bookAvailabilitySink.asFlux();
    }

    public Flux<BookAvailabilityEvent> getAvailabilityStreamForBook(Long bookId) {
        return bookAvailabilitySink.asFlux()
                .filter(event -> event.getBookId().equals(bookId));
    }
}
```

3. **BookAvailabilityController**:
```java
@RestController
@RequestMapping("/api/v1/books/availability")
public class BookAvailabilityController {
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<BookAvailabilityEvent> streamAllBookAvailability() {
        return bookAvailabilityService.getAvailabilityStream();
    }

    @GetMapping(value = "/{bookId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<BookAvailabilityEvent> streamBookAvailability(@PathVariable Long bookId) {
        return bookAvailabilityService.getAvailabilityStreamForBook(bookId);
    }
}
```

### English
The system tracks real-time changes in book availability status. This feature is used when:

- A book is borrowed
- A book is returned
- Book stock status is updated

#### Components

1. **BookAvailabilityEvent**:
```java
public class BookAvailabilityEvent {
    private Long bookId;
    private String bookTitle;
    private boolean available;
    private LocalDateTime timestamp;
    private String eventType; // BORROWED, RETURNED, UPDATED
}
```

2. **BookAvailabilityService**:
```java
@Service
public class BookAvailabilityService {
    private final Sinks.Many<BookAvailabilityEvent> bookAvailabilitySink = 
        Sinks.many().multicast().onBackpressureBuffer();

    public void notifyAvailabilityChange(Book book, String eventType) {
        BookAvailabilityEvent event = BookAvailabilityEvent.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .available(book.getAvailableCount() > 0)
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .build();

        bookAvailabilitySink.tryEmitNext(event);
    }

    public Flux<BookAvailabilityEvent> getAvailabilityStream() {
        return bookAvailabilitySink.asFlux();
    }

    public Flux<BookAvailabilityEvent> getAvailabilityStreamForBook(Long bookId) {
        return bookAvailabilitySink.asFlux()
                .filter(event -> event.getBookId().equals(bookId));
    }
}
```

3. **BookAvailabilityController**:
```java
@RestController
@RequestMapping("/api/v1/books/availability")
public class BookAvailabilityController {
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<BookAvailabilityEvent> streamAllBookAvailability() {
        return bookAvailabilityService.getAvailabilityStream();
    }

    @GetMapping(value = "/{bookId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<BookAvailabilityEvent> streamBookAvailability(@PathVariable Long bookId) {
        return bookAvailabilityService.getAvailabilityStreamForBook(bookId);
    }
}
```

## Teknik Detaylar / Technical Details

### Türkçe
#### Bağımlılıklar / Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

#### Yapılandırma / Configuration
```yaml
spring:
  webflux:
    base-path: /api/v1
```

#### Event Types
- **BORROWED**: Kitap ödünç alındığında
- **RETURNED**: Kitap iade edildiğinde
- **UPDATED**: Kitap stok durumu güncellendiğinde

### English
#### Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

#### Configuration
```yaml
spring:
  webflux:
    base-path: /api/v1
```

#### Event Types
- **BORROWED**: When a book is borrowed
- **RETURNED**: When a book is returned
- **UPDATED**: When book stock status is updated

## Best Practices

### Türkçe
1. **Backpressure Yönetimi**:
   - Sinks.many().multicast().onBackpressureBuffer() kullanımı
   - Buffer boyutunun kontrolü
   - Hata durumlarının yönetimi

2. **Performans**:
   - Event'lerin optimize edilmesi
   - Gereksiz event'lerin engellenmesi
   - Resource kullanımının kontrolü

3. **Güvenlik**:
   - Event'lerin doğrulanması
   - Yetkilendirme kontrolleri
   - Rate limiting uygulanması

### English
1. **Backpressure Management**:
   - Using Sinks.many().multicast().onBackpressureBuffer()
   - Controlling buffer size
   - Managing error cases

2. **Performance**:
   - Optimizing events
   - Preventing unnecessary events
   - Controlling resource usage

3. **Security**:
   - Validating events
   - Authorization checks
   - Implementing rate limiting

## Örnek Kullanım / Example Usage

### Türkçe
#### 1. Tüm Kitap Müsaitlik Değişikliklerini Dinleme
```bash
curl -N http://localhost:8080/api/v1/books/availability \
  -H "Authorization: Bearer {token}"
```

#### 2. Belirli Bir Kitabın Müsaitlik Değişikliklerini Dinleme
```bash
curl -N http://localhost:8080/api/v1/books/availability/1 \
  -H "Authorization: Bearer {token}"
```

### English
#### 1. Listening to All Book Availability Changes
```bash
curl -N http://localhost:8080/api/v1/books/availability \
  -H "Authorization: Bearer {token}"
```

#### 2. Listening to Specific Book Availability Changes
```bash
curl -N http://localhost:8080/api/v1/books/availability/1 \
  -H "Authorization: Bearer {token}"
``` 