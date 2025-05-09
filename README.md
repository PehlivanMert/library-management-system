# 📚 Kütüphane Yönetim Sistemi / Library Management System

## 📋 İçindekiler / Table of Contents
- [Hakkında / About](#hakkında--about)
- [Mimari / Architecture](#mimari--architecture)
- [Özellikler / Features](#özellikler--features)
- [Teknoloji Yığını / Technology Stack](#teknoloji-yığını--technology-stack)
- [Başlangıç / Getting Started](#başlangıç--getting-started)
- [Dokümantasyon / Documentation](#dokümantasyon--documentation)
- [Geliştirme / Development](#geliştirme--development)
- [Katkıda Bulunma / Contributing](#katkıda-bulunma--contributing)
- [Lisans / License](#lisans--license)

## 🎯 Hakkında / About

### Türkçe
Bu proje, Spring Boot 3.x ile geliştirilmiş modern ve güçlü bir Kütüphane Yönetim Sistemidir. Kitap yönetimi, kullanıcı yönetimi, ödünç alma işlemleri ve daha fazlasını içeren kapsamlı bir çözüm sunmaktadır.

### English
This project is a modern, robust Library Management System built with Spring Boot 3.x. It provides a comprehensive solution for managing library operations including book management, user management, borrowing operations, and more.

### Temel Özellikler / Key Features
- 📚 Tam kitap yönetim sistemi / Complete book management system
- 👥 Rol tabanlı erişim kontrolü ile kullanıcı yönetimi / User management with role-based access control
- 🔒 Güvenli kimlik doğrulama ve yetkilendirme / Secure authentication and authorization
- 📧 E-posta bildirimleri / Email notifications
- 📊 Gerçek zamanlı izleme ve metrikler / Real-time monitoring and metrics
- 🔄 Asenkron olay işleme / Asynchronous event processing
- 🎯 Yüksek test kapsamı / High test coverage

## 🏗 Mimari / Architecture

### Türkçe
Sistem, temiz ve katmanlı bir mimari desenini takip eder:

### English
The system follows a clean, layered architecture pattern:

### Temel Katmanlar / Core Layers
1. **Sunum Katmanı / Presentation Layer**
   - REST Controller'lar / REST Controllers
   - DTO'lar / DTOs
   - API Dokümantasyonu (OpenAPI/Swagger) / API Documentation

2. **İş Katmanı / Business Layer**
   - Servis uygulamaları / Service implementations
   - İş mantığı / Business logic
   - Doğrulama kuralları / Validation rules

3. **Veri Katmanı / Data Layer**
   - JPA Repository'leri / JPA Repositories
   - Entity modelleri / Entity models
   - Veritabanı migrasyonları (Flyway) / Database migrations

4. **Güvenlik Katmanı / Security Layer**
   - JWT kimlik doğrulama / JWT authentication
   - Rol tabanlı yetkilendirme / Role-based authorization
   - Güvenlik yapılandırmaları / Security configurations

<div align="left">
  <img src="docs/class.svg" alt="Class Diagram" width="1000"/>
  <br/>
  <em>Sınıf Diyagramı / Class Diagram</em>
</div>

### Altyapı Bileşenleri / Infrastructure Components
- PostgreSQL (birincil veri depolama / primary data storage)
- Redis (önbellekleme / caching)
- Kafka (olay akışı / event streaming)
- Prometheus & Grafana (izleme / monitoring)
- Flyway (veritabanı migrasyonları / database migrations)

<div align="left">
  <img src="docs/Infrastructure.svg" alt="Infrastructure Diagram" width="1000"/>
  <br/>
  <em>Altyapı Diyagramı / Infrastructure Diagram</em>
</div>

## ✨ Özellikler / Features

### Kitap Yönetimi / Book Management
- Kitap ekleme, güncelleme, silme / Add, update, delete books
- Kitap arama ve filtreleme / Search and filter books
- Kitap kullanılabilirlik takibi / Track book availability

### Kullanıcı Yönetimi / User Management
- Kullanıcı kaydı ve kimlik doğrulama / User registration and authentication
- Rol tabanlı erişim kontrolü / Role-based access control


### Ödünç Alma Sistemi / Borrowing System
- Kitap ödünç alma ve iade / Book borrowing and returning
- Son tarih yönetimi / Due date management
- Geç iade cezaları / Late return penalties


### Bildirim Sistemi / Notification System
- E-posta bildirimleri / Email notifications
- Olay tabanlı mimari / Event-driven architecture
- Kafka mesaj akışı / Kafka message streaming
- Şablon tabanlı e-postalar / Template-based emails

### İzleme ve Güvenlik / Monitoring & Security
- Gerçek zamanlı metrikler / Real-time metrics
- Güvenlik günlüğü / Security logging
- Performans izleme / Performance monitoring
- Sağlık kontrolleri / Health checks

## 🛠 Teknoloji Yığını / Technology Stack

### Backend
- Java 21
- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA
- Spring WebFlux
- Spring Mail

### Veritabanı ve Önbellekleme / Database & Caching
- PostgreSQL
- Redis
- Flyway

### Mesaj Kuyruğu / Message Queue
- Apache Kafka

### İzleme / Monitoring
- Spring Actuator
- Prometheus
- Grafana

### Test / Testing
- JUnit 5
- Mockito
- JaCoCo

### Dokümantasyon / Documentation
- OpenAPI/Swagger
- SpringDoc

### Rate Limiting
- Bucket4j

## 🚀 Başlangıç / Getting Started

### Ön Koşullar / Prerequisites
- Java 21
- Maven
- Docker & Docker Compose
- PostgreSQL
- Redis
- Kafka

### Kurulum / Installation

1. Projeyi klonlayın / Clone the repository:
```bash
git clone https://github.com/yourusername/library-management-system.git
cd library-management-system
```

2. Gerekli servisleri başlatın / Start required services:
```bash
docker-compose up -d
```

3. Projeyi derleyin / Build the project:
```bash
mvn clean install
```

4. Uygulamayı çalıştırın / Run the application:
```bash
mvn spring-boot:run
```

### Varsayılan Kimlik Bilgileri / Default Credentials
- Librarian
  - Kullanıcı adı: `librarian`
  - Şifre: `librarian123`
- Reader
  - Kullanıcı adı: `reader`
  - Şifre: `reader123`

## 📚 Dokümantasyon / Documentation

Detaylı dokümantasyon `docs` dizininde bulunmaktadır / Detailed documentation is available in the `docs` directory:

- [API Dokümantasyonu / API Documentation](docs/api.md)
- [Güvenlik Kılavuzu / Security Guide](docs/security.md)
- [Servis Katmanı / Service Layer](docs/service.md)
- [Hata Yönetimi / Exception Handling](docs/exception.md)
- [İzleme Kılavuzu / Monitoring Guide](docs/monitoring.md)
- [Migrasyon Kılavuzu / Migration Guide](docs/migration.md)
- [Kafka Entegrasyonu / Kafka Integration](docs/kafka.md)
- [E-posta Sistemi / Email System](docs/email.md)
- [Proje Yapısı / Project Structure](docs/structure.md)
- [Reaktif Programlama / Reactive Programming](docs/reactive.md)
- [Rate Limiting / İstek Sınırlama](docs/rate-limit.md)


### Testleri Çalıştırma / Running Tests
```bash
# Tüm testleri çalıştır / Run all tests
mvn test

# Kapsam ile çalıştır / Run with coverage
mvn verify
```

### Derleme / Building
```bash
# Çalıştırılabilir jar oluştur / Create executable jar
mvn clean package

# Belirli bir profil ile çalıştır / Run with specific profile
java -jar target/library-management-system.jar --spring.profiles.active=dev
```

## 🤝 Katkıda Bulunma / Contributing

1. Projeyi fork'layın / Fork the repository
2. Feature branch oluşturun / Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Değişikliklerinizi commit edin / Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Branch'inizi push edin / Push to the branch (`git push origin feature/AmazingFeature`)
5. Pull Request açın / Open a Pull Request

### Pull Request Süreci / Pull Request Process
1. README.md'yi değişikliklerle güncelleyin / Update the README.md with details of changes
2. `docs` dizinindeki dokümantasyonu güncelleyin / Update the documentation in the `docs` directory
3. Tüm testlerin geçtiğinden emin olun / Ensure all tests pass
4. Kod kapsamını %50'nin üzerinde tutun / Maintain code coverage above 50%


---

<div align="center">
  <sub>Mert Pehlivan tarafından ❤️ ile geliştirildi / Built with ❤️ by Mert Pehlivan</sub>
</div> 