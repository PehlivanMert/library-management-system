# Güvenlik Yapılandırması / Security Configuration

## Hızlı Geçiş / Quick Navigation
- [Genel Bakış / Overview](#genel-bakış--overview)
- [Güvenlik Bileşenleri / Security Components](#güvenlik-bileşenleri--security-components)
- [JWT Kimlik Doğrulama / JWT Authentication](#jwt-kimlik-doğrulama--jwt-authentication)
- [Yetkilendirme / Authorization](#yetkilendirme--authorization)
- [CORS Yapılandırması / CORS Configuration](#cors-yapılandırması--cors-configuration)
- [Best Practices](#best-practices)
- [Örnek Kullanım / Example Usage](#örnek-kullanım--example-usage)
- [Gelecek Geliştirmeler / Future Improvements](#gelecek-geliştirmeler--future-improvements)

## Genel Bakış / Overview

### Türkçe
Bu projede güvenlik için Spring Security ve JWT (JSON Web Token) kullanılmaktadır. Sistem, stateless bir mimari ile tasarlanmış olup, CORS yapılandırması ve metot seviyesi güvenlik de uygulanmıştır.

#### Güvenlik Mimarisi
1. **Kimlik Doğrulama (Authentication)**:
   - JWT tabanlı token doğrulama
   - Stateless oturum yönetimi
   - Güvenli şifre kodlama

2. **Yetkilendirme (Authorization)**:
   - Role tabanlı erişim kontrolü
   - Metot seviyesi güvenlik
   - Özel yetki kontrolleri

3. **Güvenlik Katmanları**:
   - CORS koruması
   - CSRF koruması
   - Rate limiting

### English
In this project, Spring Security and JWT (JSON Web Token) are used for security. The system is designed with a stateless architecture, and CORS configuration and method-level security are also implemented.

#### Security Architecture
1. **Authentication**:
   - JWT-based token validation
   - Stateless session management
   - Secure password encoding

2. **Authorization**:
   - Role-based access control
   - Method-level security
   - Custom permission checks

3. **Security Layers**:
   - CORS protection
   - CSRF protection
   - Rate limiting

## Güvenlik Bileşenleri / Security Components

### Türkçe
#### 1. SecurityConfig
- CORS yapılandırması
- JWT filtreleme
- Kimlik doğrulama sağlayıcısı
- Şifre kodlayıcı
- Oturum yönetimi

#### 2. JwtHelper
- Token oluşturma
- Token doğrulama
- Token'dan kullanıcı bilgisi çıkarma
- Yetki bilgisi yönetimi

#### 3. JwtAuthFilter
- JWT token kontrolü
- Yetkilendirme kontrolü
- Kullanıcı doğrulama

### English
#### 1. SecurityConfig
- CORS configuration
- JWT filtering
- Authentication provider
- Password encoder
- Session management

#### 2. JwtHelper
- Token generation
- Token validation
- User info extraction
- Authority management

#### 3. JwtAuthFilter
- JWT token verification
- Authorization check
- User authentication

## JWT Kimlik Doğrulama / JWT Authentication

### Türkçe
#### 1. Token Oluşturma
```java
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("authorities", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
    return createToken(claims, userDetails.getUsername());
}
```

#### 2. Token Doğrulama
```java
public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = getUsernameFromToken(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
}
```

#### 3. Yetki Bilgisi Çıkarma
```java
public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    List<String> authorities = (List<String>) claims.get("authorities");
    return authorities.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
}
```

### English
#### 1. Token Generation
```java
public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("authorities", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
    return createToken(claims, userDetails.getUsername());
}
```

#### 2. Token Validation
```java
public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = getUsernameFromToken(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
}
```

#### 3. Authority Extraction
```java
public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
    Claims claims = getAllClaimsFromToken(token);
    List<String> authorities = (List<String>) claims.get("authorities");
    return authorities.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
}
```

## Yetkilendirme / Authorization

### Türkçe
#### 1. Metot Seviyesi Güvenlik
```java
@PreAuthorize("hasRole('LIBRARIAN')")
public BookResponseDto createBook(BookRequestDto bookRequestDto) {
    // ...
}

@PreAuthorize("hasRole('READER')")
public LoanResponseDto borrowBook(LoanRequestDto loanRequestDto) {
    // ...
}
```

#### 2. Rol Tabanlı Erişim
- READER: Kitap ödünç alma/iade
- LIBRARIAN: Kitap ve yazar yönetimi

#### 3. Özel Yetki Kontrolleri
```java
@PreAuthorize("hasRole('LIBRARIAN') or #userId == authentication.principal.id")
public UserResponseDto updateUser(Long userId, UserRequestDto userRequestDto) {
    // ...
}
```

### English
#### 1. Method Level Security
```java
@PreAuthorize("hasRole('LIBRARIAN')")
public BookResponseDto createBook(BookRequestDto bookRequestDto) {
    // ...
}

@PreAuthorize("hasRole('READER')")
public LoanResponseDto borrowBook(LoanRequestDto loanRequestDto) {
    // ...
}
```

#### 2. Role-Based Access
- READER: Book borrowing/returning
- LIBRARIAN: Book and author management

#### 3. Custom Permission Checks
```java
@PreAuthorize("hasRole('LIBRARIAN') or #userId == authentication.principal.id")
public UserResponseDto updateUser(Long userId, UserRequestDto userRequestDto) {
    // ...
}
```

## CORS Yapılandırması / CORS Configuration

### Türkçe
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### English
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

## Best Practices

### Türkçe
1. **Token Yönetimi**:
   - Kısa token süresi
   - Yetki bilgisi token içinde
   - Token yenileme mekanizması

2. **Güvenlik Yapılandırması**:
   - CORS yapılandırması
   - CSRF koruması devre dışı
   - Metot seviyesi güvenlik

3. **Hata Yönetimi**:
   - Token doğrulama hataları
   - Yetkilendirme hataları
   - CORS hataları

### English
1. **Token Management**:
   - Short token lifetime
   - Authority info in token
   - Token refresh mechanism

2. **Security Configuration**:
   - CORS configuration
   - CSRF protection disabled
   - Method level security

3. **Error Management**:
   - Token validation errors
   - Authorization errors
   - CORS errors

## Örnek Kullanım / Example Usage

### Türkçe
#### 1. Kullanıcı Girişi
```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getUsername(),
            loginRequest.getPassword()
        )
    );
    
    String token = jwtHelper.generateToken(authentication.getPrincipal());
    return ResponseEntity.ok(new JwtResponse(token));
}
```

#### 2. Korumalı Endpoint
```java
@GetMapping("/protected")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<?> protectedEndpoint() {
    return ResponseEntity.ok("Protected endpoint accessed successfully");
}
```

### English
#### 1. User Login
```java
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getUsername(),
            loginRequest.getPassword()
        )
    );
    
    String token = jwtHelper.generateToken(authentication.getPrincipal());
    return ResponseEntity.ok(new JwtResponse(token));
}
```

#### 2. Protected Endpoint
```java
@GetMapping("/protected")
@PreAuthorize("hasRole('USER')")
public ResponseEntity<?> protectedEndpoint() {
    return ResponseEntity.ok("Protected endpoint accessed successfully");
}
```

## Gelecek Geliştirmeler / Future Improvements

### Türkçe
1. **Güvenlik İyileştirmeleri**:
   - Rate limiting
   - IP bazlı kısıtlama
   - İki faktörlü doğrulama

2. **Token Yönetimi**:
   - Refresh token
   - Token blacklist
   - Token rotasyonu

3. **Monitoring**:
   - Güvenlik olayları izleme
   - Anomali tespiti
   - Otomatik uyarılar

### English
1. **Security Improvements**:
   - Rate limiting
   - IP-based restrictions
   - Two-factor authentication

2. **Token Management**:
   - Refresh token
   - Token blacklist
   - Token rotation

3. **Monitoring**:
   - Security event monitoring
   - Anomaly detection
   - Automated alerts 