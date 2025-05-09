# Rate Limiting / İstek Sınırlama

## Genel Bakış / Overview

### Türkçe
Bu dokümantasyon, kütüphane yönetim sisteminde uygulanan rate limiting (istek sınırlama) mekanizmasını açıklamaktadır. Rate limiting, API'lerin aşırı kullanımını önlemek ve sistem kaynaklarını korumak için kullanılan önemli bir güvenlik önlemidir.

### English
This documentation explains the rate limiting mechanism implemented in the library management system. Rate limiting is an important security measure used to prevent API overuse and protect system resources.

## Teknik Detaylar / Technical Details

### Yapılandırma / Configuration

```java
@Configuration
public class RateLimitConfig {
    @Bean
    public Bucket createNewBucket() {
        // Her kullanıcı için 100 istek hakkı
        // Her 5 dakikada 100 token yenilenir
        Bandwidth limit = Bandwidth.simple(100, Duration.ofMinutes(5));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
}
```

### Limitler / Limits

#### Türkçe
- Her kullanıcı için 5 dakikada 100 istek hakkı
- Limit aşıldığında 429 (Too Many Requests) hatası
- Sadece `/api/**` endpoint'leri için geçerli

#### English
- 100 requests per minute per user
- 429 (Too Many Requests) error when limit exceeded
- Only applies to `/api/**` endpoints

## Kullanım / Usage

### Türkçe
Rate limiting otomatik olarak tüm API isteklerine uygulanır. Kullanıcılar limiti aştığında aşağıdaki yanıtı alırlar:

```json
{
    "status": 429,
    "message": "Rate limit exceeded. Please try again later."
}
```

### English
Rate limiting is automatically applied to all API requests. When users exceed the limit, they receive the following response:

```json
{
    "status": 429,
    "message": "Rate limit exceeded. Please try again later."
}
```

## Özelleştirme / Customization

### Türkçe
Rate limiting yapılandırmasını özelleştirmek için aşağıdaki değişiklikleri yapabilirsiniz:

1. **Farklı Endpoint'ler İçin Farklı Limitler**
   ```java
   registry.addInterceptor(rateLimitInterceptor)
           .addPathPatterns("/api/books/**", "/api/users/**")
           .excludePathPatterns("/api/public/**");
   ```

2. **Kullanıcı Bazlı Rate Limiting**
   ```java
   String userId = getUserIdFromRequest(request);
   Bucket userBucket = userBuckets.computeIfAbsent(userId, k -> createNewBucket());
   ```

3. **IP Bazlı Rate Limiting**
   ```java
   String ipAddress = request.getRemoteAddr();
   Bucket ipBucket = ipBuckets.computeIfAbsent(ipAddress, k -> createNewBucket());
   ```

### English
You can customize the rate limiting configuration by making the following changes:

1. **Different Limits for Different Endpoints**
   ```java
   registry.addInterceptor(rateLimitInterceptor)
           .addPathPatterns("/api/books/**", "/api/users/**")
           .excludePathPatterns("/api/public/**");
   ```

2. **User-Based Rate Limiting**
   ```java
   String userId = getUserIdFromRequest(request);
   Bucket userBucket = userBuckets.computeIfAbsent(userId, k -> createNewBucket());
   ```

3. **IP-Based Rate Limiting**
   ```java
   String ipAddress = request.getRemoteAddr();
   Bucket ipBucket = ipBuckets.computeIfAbsent(ipAddress, k -> createNewBucket());
   ```

## Bağımlılıklar / Dependencies

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
```

## Best Practices / En İyi Uygulamalar

### Türkçe
1. Rate limit değerlerini sistem kapasitesine göre ayarlayın
2. Farklı endpoint'ler için farklı limitler tanımlayın
3. Rate limit aşımında kullanıcıya anlamlı hata mesajları gösterin
4. Rate limit durumunu loglayın
5. Rate limit aşımında kullanıcıya kalan süreyi bildirin

### English
1. Adjust rate limit values according to system capacity
2. Define different limits for different endpoints
3. Show meaningful error messages when rate limit is exceeded
4. Log rate limit status
5. Inform users about remaining time when rate limit is exceeded 

## Hata Yönetimi ve Loglama / Error Handling and Logging

### Türkçe
Rate limiting sistemi, detaylı loglama ve özel hata yönetimi içerir:

1. **Loglama**
   - Her istek için IP adresi ve URI loglanır
   - Kalan token sayısı ve reset süresi loglanır
   - Rate limit aşımı durumunda uyarı logu oluşturulur

2. **Hata Yönetimi**
   - Özel `RateLimitExceededException` sınıfı
   - Detaylı hata mesajları
   - Kalan süre ve istek sayısı bilgisi
   - HTTP 429 (Too Many Requests) yanıtı

3. **Response Headers**
   - `X-Rate-Limit-Remaining`: Kalan istek sayısı
   - `X-Rate-Limit-Reset`: Limit sıfırlanma süresi
   - `X-Rate-Limit-Retry-After-Seconds`: Yeniden deneme süresi

### English
The rate limiting system includes detailed logging and custom error handling:

1. **Logging**
   - IP address and URI are logged for each request
   - Remaining tokens and reset time are logged
   - Warning log is created when rate limit is exceeded

2. **Error Handling**
   - Custom `RateLimitExceededException` class
   - Detailed error messages
   - Remaining time and request count information
   - HTTP 429 (Too Many Requests) response

3. **Response Headers**
   - `X-Rate-Limit-Remaining`: Remaining request count
   - `X-Rate-Limit-Reset`: Limit reset time
   - `X-Rate-Limit-Retry-After-Seconds`: Retry after time

### Örnek Hata Yanıtı / Example Error Response

```json
{
    "timestamp": "2024-03-14T10:30:00",
    "status": 429,
    "error": "Too Many Requests",
    "message": "Rate limit exceeded. Please try again in 45 seconds.",
    "resetTime": 45,
    "remainingRequests": 0
}
```

### Log Örnekleri / Log Examples

```
INFO  Rate limit check for IP: 192.168.1.1 and URI: /api/books
INFO  Request allowed. Remaining tokens: 99, Reset in: 60 seconds
WARN  Rate limit exceeded for IP: 192.168.1.1 and URI: /api/books. Try again in 45 seconds
``` 