# API Dokümantasyonu / API Documentation

## Hızlı Geçiş / Quick Navigation
- [Genel Bakış / Overview](#genel-bakış--overview)
- [Kimlik Doğrulama / Authentication](#kimlik-doğrulama--authentication)
- [Kitap API'leri / Book APIs](#kitap-apileri--book-apis)
- [Yazar API'leri / Author APIs](#yazar-apileri--author-apis)
- [Kullanıcı API'leri / User APIs](#kullanıcı-apileri--user-apis)
- [Ödünç API'leri / Loan APIs](#ödünç-apileri--loan-apis)
- [Hata Kodları / Error Codes](#hata-kodları--error-codes)
- [Best Practices](#best-practices)
- [Örnek Kullanım / Example Usage](#örnek-kullanım--example-usage)

## Genel Bakış / Overview

### Türkçe
Bu API, kütüphane yönetim sistemi için RESTful servisler sağlar. Tüm istekler JSON formatında yapılmalı ve yanıtlar da JSON formatında dönecektir.

#### Temel Bilgiler
- **Base URL**: `http://localhost:8080/api/v1`
- **Content-Type**: `application/json`
- **Kimlik Doğrulama**: JWT Bearer Token
- **API Versiyonu**: v1

#### Genel Endpoint Yapısı
```
GET    /api/v1/resource          # Liste
POST   /api/v1/resource          # Oluştur
GET    /api/v1/resource/{id}     # Detay
PUT    /api/v1/resource/{id}     # Güncelle
DELETE /api/v1/resource/{id}     # Sil
```

### English
This API provides RESTful services for the library management system. All requests should be made in JSON format, and responses will be returned in JSON format.

#### Basic Information
- **Base URL**: `http://localhost:8080/api/v1`
- **Content-Type**: `application/json`
- **Authentication**: JWT Bearer Token
- **API Version**: v1

#### General Endpoint Structure
```
GET    /api/v1/resource          # List
POST   /api/v1/resource          # Create
GET    /api/v1/resource/{id}     # Detail
PUT    /api/v1/resource/{id}     # Update
DELETE /api/v1/resource/{id}     # Delete
```

## Kimlik Doğrulama / Authentication

### Türkçe
#### 1. Kullanıcı Kaydı
```http
POST /api/auth/register
Content-Type: application/json

{
    "name": "John Doe",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "password123",
    "roles": ["READER"]
}
```

#### 2. Kullanıcı Girişi
```http
POST /api/auth/login
Content-Type: application/json

{
    "email": "john.doe@example.com",
    "password": "password123"
}
```

### English
#### 1. User Registration
```http
POST /api/auth/register
Content-Type: application/json

{
    "name": "John Doe",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "password123",
    "roles": ["READER"]
}
```

#### 2. User Login
```http
POST /api/auth/login
Content-Type: application/json

{
    "email": "john.doe@example.com",
    "password": "password123"
}
```

## Kitap API'leri / Book APIs

### Türkçe
#### 1. Kitap Listesi
```http
GET /api/v1/books
Authorization: Bearer {token}
```

#### 2. Kitap Oluşturma
```http
POST /api/v1/books
Authorization: Bearer {token}
Content-Type: application/json

{
    "title": "Spring Boot in Action",
    "isbn": "9781617292545",
    "stock": 10,
    "pageCount": 500,
    "publicationDate": "2020-01-01",
    "publisher": "Manning",
    "bookType": "TECHNICAL",
    "authorName": "Craig",
    "authorSurname": "Walls"
}
```

#### 3. Kitap Detayı
```http
GET /api/v1/books/{id}
Authorization: Bearer {token}
```

#### 4. Kitap Güncelleme
```http
PUT /api/v1/books/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
    "title": "Spring Boot in Action (2nd Edition)",
    "isbn": "9781617292545",
    "stock": 15,
    "pageCount": 550,
    "publicationDate": "2022-01-01",
    "publisher": "Manning",
    "bookType": "TECHNICAL",
    "authorName": "Craig",
    "authorSurname": "Walls"
}
```

#### 5. Kitap Silme
```http
DELETE /api/v1/books/{id}
Authorization: Bearer {token}
```

#### 6. Kitap Arama
```http
GET /api/v1/books/search?title=Spring&authorName=Craig&authorSurname=Walls&isbn=9781617292545&bookType=TECHNICAL&status=AVAILABLE&page=0&size=10&sort=title
Authorization: Bearer {token}
```

#### 7. Kitap Müsaitlik Takibi
```http
GET /api/v1/books/availability
Authorization: Bearer {token}
Content-Type: text/event-stream

GET /api/v1/books/availability/{bookId}
Authorization: Bearer {token}
Content-Type: text/event-stream
```

### English
#### 1. List Books
```http
GET /api/v1/books
Authorization: Bearer {token}
```

#### 2. Create Book
```http
POST /api/v1/books
Authorization: Bearer {token}
Content-Type: application/json

{
    "title": "Spring Boot in Action",
    "isbn": "9781617292545",
    "stock": 10,
    "pageCount": 500,
    "publicationDate": "2020-01-01",
    "publisher": "Manning",
    "bookType": "TECHNICAL",
    "authorName": "Craig",
    "authorSurname": "Walls"
}
```

#### 3. Get Book Details
```http
GET /api/v1/books/{id}
Authorization: Bearer {token}
```

#### 4. Update Book
```http
PUT /api/v1/books/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
    "title": "Spring Boot in Action (2nd Edition)",
    "isbn": "9781617292545",
    "stock": 15,
    "pageCount": 550,
    "publicationDate": "2022-01-01",
    "publisher": "Manning",
    "bookType": "TECHNICAL",
    "authorName": "Craig",
    "authorSurname": "Walls"
}
```

#### 5. Delete Book
```http
DELETE /api/v1/books/{id}
Authorization: Bearer {token}
```

#### 6. Search Books
```http
GET /api/v1/books/search?title=Spring&authorName=Craig&authorSurname=Walls&isbn=9781617292545&bookType=TECHNICAL&status=AVAILABLE&page=0&size=10&sort=title
Authorization: Bearer {token}
```

#### 7. Book Availability Stream
```http
GET /api/v1/books/availability
Authorization: Bearer {token}
Content-Type: text/event-stream

GET /api/v1/books/availability/{bookId}
Authorization: Bearer {token}
Content-Type: text/event-stream
```

## Yazar API'leri / Author APIs

### Türkçe
#### 1. Yazar Listesi
```http
GET /api/v1/authors
Authorization: Bearer {token}
```

#### 2. Yazar Oluşturma
```http
POST /api/v1/authors
Authorization: Bearer {token}
Content-Type: application/json

{
    "name": "Craig",
    "surname": "Walls"
}
```

#### 3. Yazar Detayı
```http
GET /api/v1/authors/{id}
Authorization: Bearer {token}
```

#### 4. Yazar Güncelleme
```http
PUT /api/v1/authors/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
    "name": "Craig",
    "surname": "Walls"
}
```

#### 5. Yazar Silme
```http
DELETE /api/v1/authors/{id}
Authorization: Bearer {token}
```

### English
#### 1. List Authors
```http
GET /api/v1/authors
Authorization: Bearer {token}
```

#### 2. Create Author
```http
POST /api/v1/authors
Authorization: Bearer {token}
Content-Type: application/json

{
    "name": "Craig",
    "surname": "Walls"
}
```

#### 3. Get Author Details
```http
GET /api/v1/authors/{id}
Authorization: Bearer {token}
```

#### 4. Update Author
```http
PUT /api/v1/authors/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
    "name": "Craig",
    "surname": "Walls"
}
```

#### 5. Delete Author
```http
DELETE /api/v1/authors/{id}
Authorization: Bearer {token}
```

## Kullanıcı API'leri / User APIs

### Türkçe
#### 1. Kullanıcı Listesi
```http
GET /api/v1/users
Authorization: Bearer {token}
```

#### 2. Kullanıcı Detayı
```http
GET /api/v1/users/{id}
Authorization: Bearer {token}
```

#### 3. Kullanıcı Güncelleme (Kütüphaneci)
```http
PUT /api/v1/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
    "name": "John Doe",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "password123",
    "roles": ["READER"]
}
```

#### 4. Kullanıcı Güncelleme (Kendisi)
```http
PUT /api/v1/users/self
Authorization: Bearer {token}
Content-Type: application/json

{
    "name": "John Doe",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "password123"
}
```

#### 5. Kullanıcı Silme
```http
DELETE /api/v1/users/{id}
Authorization: Bearer {token}
```

### English
#### 1. List Users
```http
GET /api/v1/users
Authorization: Bearer {token}
```

#### 2. Get User Details
```http
GET /api/v1/users/{id}
Authorization: Bearer {token}
```

#### 3. Update User (Librarian)
```http
PUT /api/v1/users/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
    "name": "John Doe",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "password123",
    "roles": ["READER"]
}
```

#### 4. Update User (Self)
```http
PUT /api/v1/users/self
Authorization: Bearer {token}
Content-Type: application/json

{
    "name": "John Doe",
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "password123"
}
```

#### 5. Delete User
```http
DELETE /api/v1/users/{id}
Authorization: Bearer {token}
```

## Ödünç API'leri / Loan APIs

### Türkçe
#### 1. Ödünç Listesi
```http
GET /api/v1/loans
Authorization: Bearer {token}
```

#### 2. Ödünç Oluşturma (Kütüphaneci)
```http
POST /api/v1/loans
Authorization: Bearer {token}
Content-Type: application/json

{
    "bookId": 1,
    "userId": 1,
    "borrowedDate": "2024-03-20"
}
```

#### 3. Ödünç Oluşturma (Kullanıcı)
```http
POST /api/v1/loans/user
Authorization: Bearer {token}
Content-Type: application/json

{
    "bookId": 1,
    "borrowedDate": "2024-03-20"
}
```

#### 4. Kitap İade
```http
PUT /api/v1/loans/{id}
Authorization: Bearer {token}
```

#### 5. Kullanıcı Ödünç Geçmişi
```http
GET /api/v1/loans/history/user/{userId}
Authorization: Bearer {token}
```

#### 6. Tüm Ödünç Geçmişi
```http
GET /api/v1/loans/history
Authorization: Bearer {token}
```

#### 7. Gecikmiş Ödünçler
```http
GET /api/v1/loans/late
Authorization: Bearer {token}
```

#### 8. Ödünç Detayı
```http
GET /api/v1/loans/{id}
Authorization: Bearer {token}
```

#### 9. Gecikmiş Ödünç Raporu
```http
GET /api/v1/loans/report/overdue
Authorization: Bearer {token}
```

### English
#### 1. List Loans
```http
GET /api/v1/loans
Authorization: Bearer {token}
```

#### 2. Create Loan (Librarian)
```http
POST /api/v1/loans
Authorization: Bearer {token}
Content-Type: application/json

{
    "bookId": 1,
    "userId": 1,
    "borrowedDate": "2024-03-20"
}
```

#### 3. Create Loan (User)
```http
POST /api/v1/loans/user
Authorization: Bearer {token}
Content-Type: application/json

{
    "bookId": 1,
    "borrowedDate": "2024-03-20"
}
```

#### 4. Return Book
```http
PUT /api/v1/loans/{id}
Authorization: Bearer {token}
```

#### 5. User Loan History
```http
GET /api/v1/loans/history/user/{userId}
Authorization: Bearer {token}
```

#### 6. All Loan History
```http
GET /api/v1/loans/history
Authorization: Bearer {token}
```

#### 7. Late Loans
```http
GET /api/v1/loans/late
Authorization: Bearer {token}
```

#### 8. Loan Details
```http
GET /api/v1/loans/{id}
Authorization: Bearer {token}
```

#### 9. Overdue Loan Report
```http
GET /api/v1/loans/report/overdue
Authorization: Bearer {token}
```

## Hata Kodları / Error Codes

### Türkçe
| Kod | Açıklama |
|-----|-----------|
| 200 | Başarılı |
| 201 | Oluşturuldu |
| 204 | İçerik Yok |
| 400 | Geçersiz İstek |
| 401 | Yetkisiz |
| 403 | Yasak |
| 404 | Bulunamadı |
| 409 | Çakışma |
| 500 | Sunucu Hatası |

### English
| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict |
| 500 | Server Error |

## Best Practices

### Türkçe
1. **İstek Yönetimi**:
   - Rate limiting uygulayın
   - İstek boyutunu sınırlayın
   - Timeout değerlerini ayarlayın

2. **Güvenlik**:
   - HTTPS kullanın
   - Token'ları güvenli saklayın
   - Input validasyonu yapın

3. **Performans**:
   - Önbellek kullanın
   - Sayfalama uygulayın
   - Gereksiz veri göndermeyin

### English
1. **Request Management**:
   - Implement rate limiting
   - Limit request size
   - Set timeout values

2. **Security**:
   - Use HTTPS
   - Store tokens securely
   - Validate input

3. **Performance**:
   - Use caching
   - Implement pagination
   - Don't send unnecessary data

## Örnek Kullanım / Example Usage

### Türkçe
#### 1. Kitap Ödünç Alma
```bash
# 1. Giriş yap
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","password":"password123"}'

# 2. Token'ı kaydet
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 3. Kitap ödünç al
curl -X POST http://localhost:8080/api/v1/loans/user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId":1,"borrowedDate":"2024-03-20"}'
```

### English
#### 1. Borrowing a Book
```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john.doe@example.com","password":"password123"}'

# 2. Save token
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 3. Borrow book
curl -X POST http://localhost:8080/api/v1/loans/user \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId":1,"borrowedDate":"2024-03-20"}'
``` 
``` 