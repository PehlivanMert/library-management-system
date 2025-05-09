# Proje Yapısı

## Genel Bakış
Bu proje, katmanlı mimari prensiplerini takip eden bir kütüphane yönetim sistemidir. Her katman kendi sorumluluğuna sahip olup, bağımlılıkların yönetimi ve kodun bakımı kolaylaştırılmıştır.

## Klasör Yapısı

```
src/main/java/org/pehlivan/mert/librarymanagementsystem/
├── controller/                    # Sunum Katmanı
│   ├── book/                     # Kitap API'leri
│   │   ├── BookController.java
│   │   └── BookAvailabilityController.java
│   ├── author/                   # Yazar API'leri
│   │   └── AuthorController.java
│   ├── loan/                     # Ödünç Alma API'leri
│   │   └── LoanController.java
│   └── user/                     # Kullanıcı API'leri
│       ├── UserController.java
│       └── AuthenticationController.java
│
├── service/                      # İş Mantığı Katmanı
│   ├── book/                     # Kitap Servisleri
│   │   ├── BookService.java
│   │   └── BookAvailabilityService.java
│   ├── author/                   # Yazar Servisleri
│   │   └── AuthorService.java
│   ├── loan/                     # Ödünç Alma Servisleri
│   │   └── LoanService.java
│   ├── user/                     # Kullanıcı Servisleri
│   │   ├── UserService.java
│   │   └── AuthenticationService.java
│   ├── notification/             # Bildirim Servisleri
│   │   └── NotificationService.java
│   └── email/                    # E-posta Servisleri
│       └── EmailService.java
│
├── model/                        # Veri Modeli Katmanı
│   ├── book/                     # Kitap Modelleri
│   │   ├── Book.java
│   │   └── BookAvailability.java
│   ├── author/                   # Yazar Modelleri
│   │   └── Author.java
│   ├── loan/                     # Ödünç Alma Modelleri
│   │   └── Loan.java
│   └── user/                     # Kullanıcı Modelleri
│       └── User.java
│
├── dto/                          # Veri Transfer Nesneleri
│   ├── book/                     # Kitap DTO'ları
│   │   ├── BookRequest.java
│   │   ├── BookResponse.java
│   │   └── BookAvailabilityResponse.java
│   ├── author/                   # Yazar DTO'ları
│   │   ├── AuthorRequest.java
│   │   └── AuthorResponse.java
│   ├── loan/                     # Ödünç Alma DTO'ları
│   │   ├── LoanRequest.java
│   │   └── LoanResponse.java
│   └── user/                     # Kullanıcı DTO'ları
│       ├── UserRequest.java
│       ├── UserResponse.java
│       ├── LoginRequest.java
│       └── LoginResponse.java
│
├── repository/                   # Veri Erişim Katmanı
│   ├── book/                     # Kitap Repository'leri
│   │   ├── BookRepository.java
│   │   └── BookAvailabilityRepository.java
│   ├── author/                   # Yazar Repository'leri
│   │   └── AuthorRepository.java
│   ├── loan/                     # Ödünç Alma Repository'leri
│   │   └── LoanRepository.java
│   └── user/                     # Kullanıcı Repository'leri
│       └── UserRepository.java
│
├── config/                       # Konfigürasyon Katmanı
│   ├── KafkaConfig.java          # Kafka Konfigürasyonu
│   ├── ModelMapperConfig.java    # ModelMapper Konfigürasyonu
│   ├── OpenApiConfig.java        # OpenAPI Konfigürasyonu
│   └── RedisConfig.java          # Redis Konfigürasyonu
│
├── security/                     # Güvenlik Katmanı
│   ├── config/                   # Güvenlik Konfigürasyonları
│   │   ├── SecurityConfig.java
│   │   └── JwtConfig.java
│   ├── jwt/                      # JWT İşlemleri
│       ├── JwtService.java
│       └── JwtFilter.java
│   
│
├── exception/                    # Hata Yönetimi Katmanı
│   ├── handler/                  # Hata İşleyicileri
│   │   └── GlobalExceptionHandler.java
│   └── /                    # Özel Hata Sınıfları
│       ├── BusinessException.java
│       └── TechnicalException.java

```

## Katmanlar ve Sorumluluklar

### 1. Sunum Katmanı (controller/)
- HTTP isteklerini ve yanıtlarını yönetir
- Giriş doğrulaması yapar
- İstek/Yanıt dönüşümlerini gerçekleştirir
- API dokümantasyonunu sağlar

### 2. İş Mantığı Katmanı (service/)
- İş kurallarını uygular
- İşlemleri koordine eder
- İşlem yönetimini sağlar
- Servis implementasyonlarını içerir

### 3. Veri Modeli Katmanı (model/)
- Entity sınıflarını içerir
- Veri yapılarını tanımlar
- Doğrulama kurallarını içerir

### 4. Veri Transfer Nesneleri (dto/)
- Veri transfer nesnelerini içerir
- İstek ve yanıt modellerini tanımlar
- Veri dönüşümlerini kolaylaştırır

### 5. Veri Erişim Katmanı (repository/)
- Veritabanı işlemlerini yönetir
- Sorgu implementasyonlarını içerir
- Veri kalıcılığını sağlar

### 6. Konfigürasyon Katmanı (config/)
- Uygulama konfigürasyonlarını içerir
- Harici servis entegrasyonlarını yönetir
- Sistem ayarlarını tanımlar

### 7. Güvenlik Katmanı (security/)
- Kimlik doğrulama ve yetkilendirme
- JWT işlemleri
- Güvenlik servisleri

### 8. Hata Yönetimi Katmanı (exception/)
- Özel hata sınıfları
- Global hata işleme
- Hata yanıt formatlaması

### 9. Olay Yönetimi Katmanı (event/)
- Olay üretimi ve tüketimi
- Asenkron işlem yönetimi
- Mesajlaşma sistemleri

## Best Practices

### 1. Katman Bağımlılıkları
- Her katman sadece altındaki katmanlara bağımlı olabilir
- Dairesel bağımlılıklar yasaktır
- Sorumluluklar net bir şekilde ayrılmıştır

### 2. Kod Organizasyonu
- Tutarlı paket isimlendirmesi
- Net sınıf sorumlulukları
- Düzgün dokümantasyon
- Birim test kapsamı

### 3. Güvenlik
- Giriş doğrulaması
- Kimlik doğrulama/Yetkilendirme
- Veri şifreleme
- Güvenli konfigürasyon

## Örnek Akış

### 1. Kullanıcı Kaydı
```
1. UserController (Sunum)
   - İsteği doğrular
   - DTO'ya dönüştürür
2. UserService (İş Mantığı)
   - İş kurallarını uygular
   - İşlem yönetimini sağlar
3. UserRepository (Veri Erişimi)
   - Kullanıcı verisini kaydeder
4. User (Veri Modeli)
   - Veri yapısını tanımlar
```

### 2. Kitap Yönetimi
```
1. BookController (Sunum)
   - Kitap isteklerini yönetir
2. BookService (İş Mantığı)
   - Kitap işlemlerini yönetir
3. BookRepository (Veri Erişimi)
   - Veritabanı işlemlerini gerçekleştirir
4. Book (Veri Modeli)
   - Kitap veri yapısını tanımlar
```

## Gelecek Geliştirmeler
1. **Performans İyileştirmeleri**:
   - Önbellekleme implementasyonu
   - Sorgu optimizasyonu
   - Bağlantı havuzu

2. **Ölçeklenebilirlik**:
   - Yük dengeleme
   - Veritabanı parçalama
   - Mikroservis mimarisi

3. **İzleme**:
   - Gelişmiş loglama
   - Performans metrikleri
   - Sağlık kontrolleri