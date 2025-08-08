# Changelog / Değişiklik Günlüğü

## [2.0.0] - 2025-08-08

### 🆕 Added / Eklendi
- **Refresh Token System** / Refresh Token Sistemi
  - Token rotation (her refresh'te yeni token)
  - Token blacklisting (logout sonrası)
  - Scheduled cleanup (expired tokens)
  - UUID-based refresh tokens
  - New endpoints: `/api/v1/auth/refresh`, `/api/v1/auth/logout`

- **Performance Optimizations** / Performans Optimizasyonları
  - EntityGraph support for N+1 problem solution
  - LEFT JOIN FETCH optimization
  - Pagination support for large datasets
  - Custom queries for better performance
  - New endpoints: `/api/v1/books/paged`, `/api/v1/books/fetch`

- **Security Enhancements** / Güvenlik İyileştirmeleri
  - Public registration endpoint (`/api/v1/auth/register`)
  - Role-based access control improvements
  - Custom exception handling
  - UnauthorizedRoleException for better security

- **Monitoring Improvements** / İzleme İyileştirmeleri
  - Enhanced Grafana dashboard (8 panels)
  - Real-time updates (10-second refresh)
  - HTTP performance metrics
  - Business metrics tracking
  - Improved Prometheus configuration

### 🔧 Changed / Değiştirildi
- **Authentication Response Format** / Kimlik Doğrulama Yanıt Formatı
  - Login response now includes refresh token
  - Token type and expiration information
  - Username and email in response

- **User Registration Flow** / Kullanıcı Kayıt Akışı
  - Public registration limited to READER role
  - LIBRARIAN creation requires authentication
  - Better error messages for unauthorized roles

- **Book Service** / Kitap Servisi
  - Added pagination methods
  - EntityGraph integration
  - Performance optimizations

### 🐛 Fixed / Düzeltildi
- **Compilation Errors** / Derleme Hataları
  - Missing imports for PreAuthorize
  - UserRegistrationNotification userId field
  - Constructor usage in services
  - Role enum imports

- **Security Issues** / Güvenlik Sorunları
  - Refresh token validation logic
  - Access denied exception handling
  - Custom error messages

### 📚 Documentation / Dokümantasyon
- **Updated README.md** / README.md Güncellendi
  - New features section
  - Updated endpoints list
  - Installation instructions
  - Usage examples

- **Enhanced API Documentation** / API Dokümantasyonu Geliştirildi
  - New authentication endpoints
  - Pagination endpoints
  - Response format examples

- **Updated Service Documentation** / Servis Dokümantasyonu Güncellendi
  - RefreshTokenService documentation
  - BookService pagination methods
  - Performance optimization details

- **Monitoring Documentation** / İzleme Dokümantasyonu
  - New dashboard features
  - Enhanced metrics tracking
  - Real-time monitoring setup

### 🧪 Testing / Test
- **New Test Cases** / Yeni Test Durumları
  - RefreshTokenService tests
  - Pagination endpoint tests
  - Authentication flow tests
  - Performance optimization tests

### 🔄 Migration / Migrasyon
- **Database Changes** / Veritabanı Değişiklikleri
  - New refresh_tokens table
  - Flyway migration script
  - Index optimizations

## [1.0.0] - 2025-01-01

### 🆕 Initial Release / İlk Sürüm
- Basic library management system
- JWT authentication
- Role-based access control
- Book and user management
- Loan system
- Email notifications
- Kafka integration
- Basic monitoring setup

---

## Versioning / Sürüm Numaralandırma

Bu proje [SemVer](http://semver.org/) kullanır. Sürüm numaraları şu formatta olur: `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes / Uyumsuz değişiklikler
- **MINOR**: New features / Yeni özellikler
- **PATCH**: Bug fixes / Hata düzeltmeleri

## Release Notes / Sürüm Notları

### v2.0.0 Breaking Changes / Uyumsuz Değişiklikler
- Authentication response format changed
- New database table (refresh_tokens)
- Updated API endpoints structure

### Migration Guide / Migrasyon Kılavuzu
1. Run database migrations
2. Update client applications for new auth response format
3. Configure new monitoring dashboard
4. Test new pagination endpoints

---

<div align="center">
  <sub>Bu changelog, projenin tüm önemli değişikliklerini takip eder / This changelog tracks all significant changes to the project</sub>
</div>
