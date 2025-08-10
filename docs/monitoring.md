# Monitoring ve Metrics / Monitoring and Metrics

## Hızlı Geçiş / Quick Navigation
- [Genel Bakış / Overview](#genel-bakış--overview)
- [Neden Monitoring? / Why Monitoring?](#neden-monitoring--why-monitoring)
- [Yapılandırma / Configuration](#yapılandırma--configuration)
- [İzlenen Metrikler / Monitored Metrics](#izlenen-metrikler--monitored-metrics)
- [Loglama / Logging](#loglama--logging)
- [Best Practices](#best-practices)
- [Örnek Kullanım Senaryoları / Example Use Cases](#örnek-kullanım-senaryoları--example-use-cases)
- [Gelecek Geliştirmeler / Future Improvements](#gelecek-geliştirmeler--future-improvements)

## Genel Bakış / Overview

### Türkçe
Bu projede sistem metriklerini izlemek için Spring Boot Actuator, Prometheus ve Grafana kullanılmaktadır. Toplanan metrikler Prometheus'a aktarılmakta ve Grafana ile görselleştirilmektedir.

#### Monitoring Nedir?
Monitoring, sistemin performansını, sağlığını ve davranışını sürekli olarak izleme ve analiz etme sürecidir. Bu süreç:
1. Sistem performansının gerçek zamanlı izlenmesini
2. Hataların hızlıca tespit edilmesini
3. Sistem kaynaklarının optimize edilmesini sağlar
4. Bunlara ek olarak, loglarda log klasörü altında oluşturulmaktadır.

#### Neden Bu Araçlar?
1. **Spring Boot Actuator**: 
   - Hazır metrikler
   - Health checks
   - Kolay entegrasyon

2. **Prometheus**:
   - Time-series veritabanı
   - Güçlü sorgulama dili
   - Alerting desteği

3. **Grafana**:
   - Zengin görselleştirme
   - Dashboard özelleştirme
   - Real-time monitoring

### English
In this project, Spring Boot Actuator, Prometheus, and Grafana are used to monitor system metrics. The collected metrics are exported to Prometheus and visualized with Grafana.

#### What is Monitoring?
Monitoring is the process of continuously observing and analyzing system performance, health, and behavior. This process:
1. Enables real-time monitoring of system performance
2. Allows quick detection of issues
3. Helps optimize system resources

#### Why These Tools?
1. **Spring Boot Actuator**:
   - Ready-to-use metrics
   - Health checks
   - Easy integration

2. **Prometheus**:
   - Time-series database
   - Powerful query language
   - Alerting support

3. **Grafana**:
   - Rich visualization
   - Dashboard customization
   - Real-time monitoring

## Neden Monitoring? / Why Monitoring?

### Türkçe
1. **Performans İzleme**:
   - HTTP istek süreleri
   - Veritabanı sorgu performansı
   - Cache hit/miss oranları
   - Sistem kaynak kullanımı

2. **Hata Tespiti**:
   - Exception sayıları
   - Error rate trendi
   - Başarısız işlem oranları
   - Sistem hataları

3. **Kapasite Planlama**:
   - Kaynak kullanım trendi
   - Sistem limitleri
   - Ölçeklendirme ihtiyaçları
   - Yük dengeleme

4. **Kullanıcı Davranışı**:
   - API kullanım istatistikleri
   - Kullanıcı aktiviteleri
   - Popüler özellikler
   - Performans etkileri

### English
1. **Performance Monitoring**:
   - HTTP request durations
   - Database query performance
   - Cache hit/miss ratios
   - System resource usage

2. **Error Detection**:
   - Exception counts
   - Error rate trends
   - Failed operation rates
   - System errors

3. **Capacity Planning**:
   - Resource usage trends
   - System limits
   - Scaling needs
   - Load balancing

4. **User Behavior**:
   - API usage statistics
   - User activities
   - Popular features
   - Performance impacts

## Yapılandırma / Configuration

### Türkçe
#### 1. Spring Boot Actuator
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
    endpoint:
      health:
        show-details: always
      prometheus:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        descriptions: true
        step: 1m
    tags:
      application: ${spring.application.name}
      instance: ${spring.application.name}-${random.value}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      sla:
        http.server.requests: 10ms,50ms,100ms,200ms,500ms,1s,2s,5s
```

#### 2. Prometheus
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  scrape_timeout: 10s
  external_labels:
    monitor: 'library-management-system'
    environment: 'development'

scrape_configs:
  - job_name: 'library-management-system'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['127.0.0.1:8080']
        labels:
          application: 'library-management-system'
          instance: 'library-app-1'
          environment: 'development'
    scheme: 'http'
    scrape_interval: 15s
    honor_labels: false
    scrape_timeout: 10s
    params:
      format: ['prometheus']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: '(.*):.*'
        replacement: '$1'
      - source_labels: [__meta_kubernetes_pod_name]
        target_label: pod
      - source_labels: [__meta_kubernetes_namespace]
        target_label: namespace
```

### English
#### 1. Spring Boot Actuator
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
    endpoint:
      health:
        show-details: always
      prometheus:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
        descriptions: true
        step: 1m
    tags:
      application: ${spring.application.name}
      instance: ${spring.application.name}-${random.value}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      sla:
        http.server.requests: 10ms,50ms,100ms,200ms,500ms,1s,2s,5s
```

#### 2. Prometheus
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  scrape_timeout: 10s

scrape_configs:
  - job_name: 'library-management'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    scheme: 'http'
    scrape_interval: 15s
    honor_labels: true

#### 3. Grafana
Grafana, Prometheus ile entegre edilerek metriklerin görselleştirilmesini sağlar. Grafana yapılandırması otomatik olarak sağlanmaktadır:

**Otomatik Yapılandırma:**
- **Dashboard Provisioning**: `grafana/provisioning/dashboards/library.yml`
- **Datasource Provisioning**: `grafana/provisioning/datasources/prometheus.yml`
- **Dashboard JSON**: `grafana-library-monitoring-dashboard.json`

**Manuel Import (Alternatif):**
- Dashboard'u manuel olarak import etmek için `grafana-library-monitoring-dashboard.json` dosyasını Grafana'ya yükleyin

**Dashboard Özellikleri (v3.0):**
- **20 farklı panel** kapsamlı monitoring dashboard'u
- **Real-time updates** (10 saniye refresh rate)
- **Smooth line interpolation** ve fill opacity
- **Table format legend'lar** ve responsive tasarım
- **Dark theme** ve modern UI
- **Emoji'ler** ile görsel iyileştirmeler
- **HTTP performance metrics** (request rate, response time percentiles)
- **Business metrics** (books, users, authors, loans)
- **Error monitoring** ve system health tracking
- **Templating variables** (HTTP methods, status codes)
- **Comprehensive analytics** ve trend analysis
```

#### Grafana Dashboard Görselleri

**Dashboard 1 - Ana Monitoring Paneli:**
![Grafana Dashboard 1](img/Grafana_Dashboard1.png)

Bu dashboard, sistemin genel durumunu gösterir:
- HTTP Status Code Distribution
- Request Method Distribution
- Book Category Distribution
- Loan Status Overview
- User Role Distribution
- System Performance Trends
- Error Rate Monitoring

**Dashboard 2 - Detaylı Metrikler:**
![Grafana Dashboard 2](img/Grafana_Dashboard2.png)

Bu dashboard, daha detaylı metrikleri içerir:
- Author Performance Metrics
- Loan Management Overview
- Email Communication Metrics
- Notification System Performance

**Dashboard 3 - Sistem Genel Bakış:**
![Grafana Dashboard 3](img/Grafana_Dashboard3.png)

Bu dashboard, sistemin genel bakışını sağlar:
- System Overview (System Health, Total Books, Total Users, Total Authors)
- HTTP Request Rate (Last 5m)
- HTTP Response Time Percentiles
- Book Statistics Overview
- User Activity Statistics

### 📊 Güncel Dashboard Paneli Detayları (v3.0)

**Ana Paneller (20 Panel):**

1. **🏠 System Overview** - Sistem sağlığı durumu
2. **📚 Total Books** - Toplam kitap sayısı
3. **👥 Total Users** - Toplam kullanıcı sayısı
4. **👨‍💼 Total Authors** - Toplam yazar sayısı
5. **🌐 HTTP Request Rate** - HTTP istek oranları (son 5 dakika)
6. **⏱️ HTTP Response Time Percentiles** - Yanıt süresi yüzdelikleri
7. **📊 Book Statistics Overview** - Kitap istatistikleri
8. **👥 User Activity Statistics** - Kullanıcı aktivite istatistikleri
9. **👨‍💼 Author Performance Metrics** - Yazar performans metrikleri
10. **📋 Loan Management Overview** - Ödünç alma yönetimi
11. **📧 Email Communication Metrics** - Email iletişim metrikleri
12. **🔔 Notification System Performance** - Bildirim sistemi performansı
13. **📈 HTTP Status Code Distribution** - HTTP durum kodu dağılımı
14. **🔄 Request Method Distribution** - İstek metodu dağılımı
15. **📊 Book Category Distribution** - Kitap kategori dağılımı (Pie Chart)
16. **📋 Loan Status Overview** - Ödünç alma durumu (Pie Chart)
17. **👥 User Role Distribution** - Kullanıcı rol dağılımı (Pie Chart)
18. **📈 System Performance Trends** - Sistem performans trendleri
19. **🔍 Error Rate Monitoring** - Hata oranı izleme
20. **📊 Business Metrics Summary** - İş metrikleri özeti

**Özellikler:**
- **Templating Variables**: HTTP Method ve HTTP Status filtreleme
- **Real-time Updates**: 10 saniye otomatik yenileme
- **Dark Theme**: Modern koyu tema
- **Responsive Design**: Mobil ve masaüstü uyumlu
- **Interactive Panels**: Tıklanabilir ve detay görüntüleme

## İzlenen Metrikler / Monitored Metrics

### Türkçe
#### 1. Sistem Metrikleri
- JVM metrikleri (heap, threads, GC)
- HTTP istekleri (count, duration, errors)
- Sistem kaynakları (CPU, Memory, Disk)
- Thread kullanımı (active, daemon, peak)

#### 2. Uygulama Metrikleri
- HTTP istek süreleri
- Veritabanı sorgu süreleri
- Cache hit/miss oranları
- Hata sayıları ve oranları

#### 3. İş Metrikleri
- Kullanıcı kayıtları
- Kitap ödünç alma/iade
- Yazar ve kitap sayıları
- Gecikmiş kitaplar

#### 4. Kapsamlı Metrikler (v3.0)
- **Sistem Sağlığı Metrikleri**:
  - System overview ve health status
  - HTTP request/response monitoring
  - Error rate tracking
- **İş Metrikleri**:
  - Kitap, kullanıcı, yazar sayıları
  - Ödünç alma/iade istatistikleri
  - Gecikmiş kitap takibi
- **Performans Metrikleri**:
  - HTTP response time percentiles
  - Request rate monitoring
  - System performance trends
- **İletişim Metrikleri**:
  - Email gönderim istatistikleri
  - Notification system performance
  - Error tracking
- **Analitik Metrikleri**:
  - Category distribution
  - User role distribution
  - Loan status overview

### English
#### 1. System Metrics
- JVM metrics (heap, threads, GC)
- HTTP requests (count, duration, errors)
- System resources (CPU, Memory, Disk)
- Thread usage (active, daemon, peak)

#### 2. Application Metrics
- HTTP request durations
- Database query durations
- Cache hit/miss ratios
- Error counts and rates

#### 3. Business Metrics
- User registrations
- Book loans/returns
- Author and book counts
- Overdue books

#### 4. Comprehensive Metrics (v3.0)
- **System Health Metrics**:
  - System overview and health status
  - HTTP request/response monitoring
  - Error rate tracking
- **Business Metrics**:
  - Book, user, author counts
  - Loan/return statistics
  - Overdue book tracking
- **Performance Metrics**:
  - HTTP response time percentiles
  - Request rate monitoring
  - System performance trends
- **Communication Metrics**:
  - Email sending statistics
  - Notification system performance
  - Error tracking
- **Analytics Metrics**:
  - Category distribution
  - User role distribution
  - Loan status overview

## Loglama / Logging

### Türkçe
#### 1. Log Yapılandırması
```yaml
logging:
  level:
    org.pehlivan.mert.librarymanagementsystem: DEBUG
    org.springframework.web: INFO
    org.springframework.security: INFO
    org.springframework.data: INFO
    org.hibernate: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/library-management.log
    max-size: 10MB
    max-history: 10
    total-size-cap: 100MB
```

#### 2. Log Seviyeleri
- DEBUG: Uygulama detayları
- INFO: Genel bilgiler
- WARN: Uyarılar
- ERROR: Hatalar

### English
#### 1. Log Configuration
```yaml
logging:
  level:
    org.pehlivan.mert.librarymanagementsystem: DEBUG
    org.springframework.web: INFO
    org.springframework.security: INFO
    org.springframework.data: INFO
    org.hibernate: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/library-management.log
    max-size: 10MB
    max-history: 10
    total-size-cap: 100MB
```

#### 2. Log Levels
- DEBUG: Application details
- INFO: General information
- WARN: Warnings
- ERROR: Errors

## Best Practices

### Türkçe
1. **Metric İsimlendirme**:
   - Anlamlı isimler
   - Tutarlı format
   - Uygun etiketler

2. **Log Yönetimi**:
   - Uygun log seviyeleri
   - Yapılandırılmış loglama
   - Log rotasyonu

3. **Performans İzleme**:
   - HTTP istek süreleri
   - Veritabanı sorgu süreleri
   - Cache performansı

### English
1. **Metric Naming**:
   - Meaningful names
   - Consistent format
   - Appropriate labels

2. **Log Management**:
   - Appropriate log levels
   - Structured logging
   - Log rotation

3. **Performance Monitoring**:
   - HTTP request durations
   - Database query durations
   - Cache performance

## Örnek Kullanım Senaryoları / Example Use Cases

### Türkçe
1. **Performans İzleme**:
   - HTTP istek süreleri
   - Veritabanı sorgu süreleri
   - Cache hit/miss oranları

2. **Hata Tespiti**:
   - Exception sayıları
   - Error rate trendi
   - Başarısız işlem oranları

3. **Kapasite Planlama**:
   - Kaynak kullanım trendi
   - Sistem limitleri
   - Ölçeklendirme ihtiyaçları

### English
1. **Performance Monitoring**:
   - HTTP request durations
   - Database query durations
   - Cache hit/miss ratios

2. **Error Detection**:
   - Exception counts
   - Error rate trends
   - Failed operation rates

3. **Capacity Planning**:
   - Resource usage trends
   - System limits
   - Scaling needs

## Gelecek Geliştirmeler / Future Improvements

### Türkçe
1. **Yeni Metrikler**:
   - Cache performansı
   - API kullanım istatistikleri
   - Kullanıcı davranış analizi

2. **Gelişmiş İzleme**:
   - Anomali tespiti
   - Otomatik ölçeklendirme
   - Trend analizi

3. **Log İyileştirmeleri**:
   - Merkezi log yönetimi
   - Log analizi
   - Otomatik uyarılar

### English
1. **New Metrics**:
   - Cache performance
   - API usage statistics
   - User behavior analysis

2. **Advanced Monitoring**:
   - Anomaly detection
   - Auto-scaling
   - Trend analysis

3. **Log Improvements**:
   - Centralized log management
   - Log analysis
   - Automated alerts

## Dashboard Kullanım Kılavuzu / Dashboard Usage Guide

### Türkçe
#### Dashboard Erişimi
1. Grafana'ya giriş yapın: http://localhost:3000
2. Sol menüden "Dashboards" seçin
3. "Library Management System - Comprehensive Monitoring Dashboard"u açın

#### Otomatik Yapılandırma
Grafana, Docker Compose ile başlatıldığında otomatik olarak yapılandırılır:

**Provisioning Dosyaları:**
- `grafana/provisioning/dashboards/library.yml` - Dashboard provisioning
- `grafana/provisioning/datasources/prometheus.yml` - Prometheus datasource
- `grafana-library-monitoring-dashboard.json` - Dashboard tanımı

**Otomatik Özellikler:**
- Dashboard otomatik olarak yüklenir
- Prometheus datasource otomatik olarak eklenir
- Dashboard düzenlenebilir ve özelleştirilebilir

#### Dashboard Özellikleri
- **Real-time Updates**: 5 saniyede bir otomatik yenileme
- **Time Range**: Son 15 dakika varsayılan, özelleştirilebilir
- **Responsive Design**: Mobil ve masaüstü uyumlu
- **Dark Theme**: Göz yorgunluğunu azaltan koyu tema

#### Panel Açıklamaları
1. **System Overview**: Sistem sağlığı ve genel istatistikler
2. **HTTP Metrics**: İstek oranları ve yanıt süreleri
3. **Business Metrics**: Kitap, kullanıcı ve ödünç alma istatistikleri
4. **Performance Metrics**: Sistem performans göstergeleri

### English
#### Dashboard Access
1. Login to Grafana: http://localhost:3000
2. Select "Dashboards" from the left menu
3. Open "Library Management System - Comprehensive Monitoring Dashboard"

#### Dashboard Features
- **Real-time Updates**: Auto-refresh every 10 seconds
- **Time Range**: Default 1 hour, customizable
- **Responsive Design**: Mobile and desktop compatible
- **Dark Theme**: Dark theme reducing eye strain
- **Templating Variables**: HTTP Method and Status filtering
- **Interactive Panels**: Clickable panels with detailed views

#### Panel Descriptions
1. **System Overview**: System health and general statistics
2. **HTTP Metrics**: Request rates and response times
3. **Business Metrics**: Book, user, and loan statistics
4. **Performance Metrics**: System performance indicators
5. **Communication Metrics**: Email and notification statistics
6. **Analytics Metrics**: Category and role distributions
7. **Error Monitoring**: Error rates and system issues

## Mevcut Monitoring Kurulumu / Current Monitoring Setup

### Türkçe
Proje, kapsamlı bir monitoring sistemi ile birlikte gelir:

**Docker Compose ile Otomatik Kurulum:**
```bash
# Tüm servisleri başlat (Prometheus, Grafana, Application)
docker-compose up -d

# Servislerin durumunu kontrol et
docker-compose ps

# Grafana'ya erişim: http://localhost:3000
# Prometheus'a erişim: http://localhost:9090
# Application'a erişim: http://localhost:8080
```

**Otomatik Yapılandırma:**
- Prometheus otomatik olarak uygulamadan metrikleri toplar
- Grafana otomatik olarak dashboard'u yükler
- Datasource otomatik olarak yapılandırılır

## Manuel Dashboard Yükleme / Manual Dashboard Import

### Türkçe
Grafana dashboard'unu manuel olarak yüklemek için aşağıdaki adımları takip edin:

#### 1. Grafana'ya Erişim
```bash
# Grafana'nın çalıştığını kontrol edin
docker ps | grep grafana

# Grafana URL: http://localhost:3000
# Kullanıcı adı: admin
# Şifre: admin
```

#### 2. cURL ile Dashboard Yükleme
```bash
# Dashboard'u cURL ile yükleyin
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @grafana-library-monitoring-dashboard.json
```

#### 3. Başarılı Yükleme Yanıtı
```json
{
  "folderUid": "",
  "id": 4,
  "slug": "ad2f435",
  "status": "success",
  "uid": "library-monitoring-dashboard",
  "url": "/d/library-monitoring-dashboard/ad2f435",
  "version": 1
}
```

#### 4. Dashboard'a Erişim
- **Ana URL:** http://localhost:3000
- **Dashboard URL:** http://localhost:3000/d/library-monitoring-dashboard/ad2f435

#### 5. Troubleshooting
Eğer dashboard yükleme sırasında hata alırsanız:

```bash
# Grafana loglarını kontrol edin
docker logs library-grafana --tail=20

# Dashboard JSON dosyasının geçerliliğini kontrol edin
cat grafana-library-monitoring-dashboard.json | jq .

# Grafana'yı yeniden başlatın
docker restart library-grafana
```

### English
The project comes with a comprehensive monitoring system:

**Automatic Setup with Docker Compose:**
```bash
# Start all services (Prometheus, Grafana, Application)
docker-compose up -d

# Check service status
docker-compose ps

# Access Grafana: http://localhost:3000
# Access Prometheus: http://localhost:9090
# Access Application: http://localhost:8080
```

**Automatic Configuration:**
- Prometheus automatically collects metrics from the application
- Grafana automatically loads the dashboard
- Datasource is automatically configured

### English
Follow the steps below to manually import the Grafana dashboard:

#### 1. Access Grafana
```bash
# Check if Grafana is running
docker ps | grep grafana

# Grafana URL: http://localhost:3000
# Username: admin
# Password: admin
```

#### 2. Import Dashboard with cURL
```bash
# Import dashboard using cURL
curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
  -H "Content-Type: application/json" \
  -d @grafana-library-monitoring-dashboard.json
```

#### 3. Successful Import Response
```json
{
  "folderUid": "",
  "id": 4,
  "slug": "ad2f435",
  "status": "success",
  "uid": "library-monitoring-dashboard",
  "url": "/d/library-monitoring-dashboard/ad2f435",
  "version": 1
}
```

#### 4. Access Dashboard
- **Main URL:** http://localhost:3000
- **Dashboard URL:** http://localhost:3000/d/library-monitoring-dashboard/ad2f435

#### 5. Troubleshooting
If you encounter errors during dashboard import:

```bash
# Check Grafana logs
docker logs library-grafana --tail=20

# Validate dashboard JSON file
cat grafana-library-monitoring-dashboard.json | jq .

# Restart Grafana
docker restart library-grafana
``` 