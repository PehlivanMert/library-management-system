# Application Configuration Guide

Bu doküman, uygulamamızın `application.yml` dosyasındaki yapılandırmaları açıklamaktadır.

## Server Configuration
```yaml
server:
  port: ${SERVER_PORT:8080}
  servlet:
    context-path: /
```
- Uygulama 8080 portunda çalışır (SERVER_PORT environment variable ile değiştirilebilir)
- Context path root (/) olarak ayarlanmıştır

## Spring Configuration

### Application Info
```yaml
spring:
  application:
    name: library-management-system
  info:
    build:
      creator: Mert Pehlivan
```
- Uygulama adı ve creator bilgisi tanımlanmıştır

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:userdb}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:root}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1800000
```
- PostgreSQL veritabanı bağlantısı
- HikariCP connection pool yapılandırması
- Environment variables ile özelleştirilebilir bağlantı bilgileri

### JPA Configuration
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        enable_lazy_load_no_trans: true
```
- Hibernate otomatik şema güncellemesi
- SQL logları aktif
- Lazy loading desteği

### Flyway Migration
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```
- Database migration için Flyway aktif
- Migration dosyaları `db/migration` klasöründe

### Email Configuration
```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:pehlivannmert@gmail.com}
    password: ${MAIL_PASSWORD:mtytatspsqgiwrom}
```
- Gmail SMTP sunucusu kullanılıyor
- Environment variables ile özelleştirilebilir mail ayarları

### Redis Configuration
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:pass}
```
- Redis cache yapılandırması
- Connection pool ve timeout ayarları
- Environment variables ile özelleştirilebilir

## Management & Monitoring

### Actuator Endpoints
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
```
- Health check, metrics ve prometheus endpointleri aktif
- Base path: /actuator

### Metrics Configuration
```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
```
- Prometheus metrics export aktif
- HTTP request metrikleri detaylı olarak toplanıyor

## API Documentation

### Swagger/OpenAPI
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```
- Swagger UI aktif
- API dokümantasyonu için OpenAPI 3.0 kullanılıyor

## Security

### JWT Configuration
```yaml
jwt:
  secret: ${JWT_SECRET:Yp/KI2jI5L7yM7xmcnVCGW4jO2smrdXknmXWeQ/rhaY=}
  expiration: ${JWT_EXPIRATION:3600000}
```
- JWT token yapılandırması
- Token süresi 1 saat (3600000 ms)
- Environment variables ile özelleştirilebilir

## Logging Configuration
```yaml
logging:
  level:
    org.pehlivan.mert.librarymanagementsystem: DEBUG
  file:
    name: logs/library-management.log
    max-size: 10MB
    max-history: 10
```
- Log seviyesi DEBUG
- Log dosyası boyut ve rotasyon ayarları
- Pattern ve format tanımlamaları

## Kafka Configuration
```yaml
kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  producer:
    key-serializer: org.apache.kafka.common.serialization.StringSerializer
    value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  consumer:
    group-id: ${KAFKA_CONSUMER_GROUP:notification-group}
```
- Kafka producer ve consumer yapılandırması
- JSON serileştirme kullanılıyor
- Environment variables ile özelleştirilebilir

## Environment Variables

Uygulama aşağıdaki environment variables ile özelleştirilebilir:

- `SERVER_PORT`: Uygulama portu (default: 8080)
- `DB_HOST`: Veritabanı host (default: localhost)
- `DB_PORT`: Veritabanı port (default: 5432)
- `DB_NAME`: Veritabanı adı (default: userdb)
- `DB_USERNAME`: Veritabanı kullanıcı adı (default: postgres)
- `DB_PASSWORD`: Veritabanı şifresi (default: root)
- `MAIL_HOST`: Mail sunucusu (default: smtp.gmail.com)
- `MAIL_PORT`: Mail port (default: 587)
- `MAIL_USERNAME`: Mail kullanıcı adı
- `MAIL_PASSWORD`: Mail şifresi
- `REDIS_HOST`: Redis host (default: localhost)
- `REDIS_PORT`: Redis port (default: 6379)
- `REDIS_PASSWORD`: Redis şifresi (default: pass)
- `JWT_SECRET`: JWT secret key
- `JWT_EXPIRATION`: JWT token süresi (default: 3600000 ms)
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka sunucuları (default: localhost:9092)
- `KAFKA_CONSUMER_GROUP`: Kafka consumer group (default: notification-group) 