# Hata Yönetimi / Exception Handling

## Hızlı Geçiş / Quick Navigation
- [Genel Bakış / Overview](#genel-bakış--overview)
- [Hata Yapısı / Error Structure](#hata-yapısı--error-structure)
- [Özel Hatalar / Custom Exceptions](#özel-hatalar--custom-exceptions)
- [Global Hata Yönetimi / Global Exception Handler](#global-hata-yönetimi--global-exception-handler)
- [Hata Kodları / Error Codes](#hata-kodları--error-codes)
- [Best Practices](#best-practices)
- [Örnek Kullanım / Example Usage](#örnek-kullanım--example-usage)
- [Gelecek Geliştirmeler / Future Improvements](#gelecek-geliştirmeler--future-improvements)

## Genel Bakış / Overview

### Türkçe
Bu projede hata yönetimi için özel exception sınıfları ve global exception handler kullanılmaktadır. Sistem, hataları yapılandırılmış bir formatta döndürerek istemci tarafında daha iyi bir hata yönetimi sağlar.

#### Hata Yönetimi Mimarisi
1. **Özel Exception Sınıfları**:
   - Domain-specific hatalar
   - İş mantığı hataları
   - Validasyon hataları

2. **Global Exception Handler**:
   - Merkezi hata yakalama
   - Standart hata formatı
   - HTTP durum kodları

3. **Hata Yapısı**:
   - Hata kodu
   - Hata mesajı
   - Detaylı bilgi
   - Zaman damgası

### English
In this project, custom exception classes and a global exception handler are used for error management. The system returns errors in a structured format, providing better error handling on the client side.

#### Error Management Architecture
1. **Custom Exception Classes**:
   - Domain-specific errors
   - Business logic errors
   - Validation errors

2. **Global Exception Handler**:
   - Centralized error catching
   - Standard error format
   - HTTP status codes

3. **Error Structure**:
   - Error code
   - Error message
   - Detailed information
   - Timestamp

## Hata Yapısı / Error Structure

### Türkçe
```java
public class ErrorResponse {
    private String code;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
}
```

#### Alanlar
1. **code**: Hata kodu (örn: "BOOK_NOT_FOUND")
2. **message**: Kullanıcı dostu hata mesajı
3. **details**: Teknik detaylar
4. **timestamp**: Hata zamanı
5. **path**: Hatanın oluştuğu endpoint

### English
```java
public class ErrorResponse {
    private String code;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
}
```

#### Fields
1. **code**: Error code (e.g., "BOOK_NOT_FOUND")
2. **message**: User-friendly error message
3. **details**: Technical details
4. **timestamp**: Error time
5. **path**: Endpoint where error occurred

## Özel Hatalar / Custom Exceptions

### Türkçe
#### 1. Kitap Hataları
```java
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }
}

public class BookAlreadyExistsException extends RuntimeException {
    public BookAlreadyExistsException(String message) {
        super(message);
    }
}
```

#### 2. Kullanıcı Hataları
```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
```

#### 3. Ödünç Alma Hataları
```java
public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String message) {
        super(message);
    }
}

public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(String message) {
        super(message);
    }
}
```

### English
#### 1. Book Errors
```java
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String message) {
        super(message);
    }
}

public class BookAlreadyExistsException extends RuntimeException {
    public BookAlreadyExistsException(String message) {
        super(message);
    }
}
```

#### 2. User Errors
```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
```

#### 3. Loan Errors
```java
public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String message) {
        super(message);
    }
}

public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException(String message) {
        super(message);
    }
}
```

## Global Hata Yönetimi / Global Exception Handler

### Türkçe
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBookNotFoundException(BookNotFoundException ex, WebRequest request) {
        log.error("Book not found: {}", ex.getMessage());
        return ErrorResponse.builder()
                .code("BOOK_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(((ServletWebRequest) request).getRequest().getRequestURI())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        return ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed")
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(((ServletWebRequest) request).getRequest().getRequestURI())
                .build();
    }
}
```

### English
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBookNotFoundException(BookNotFoundException ex, WebRequest request) {
        log.error("Book not found: {}", ex.getMessage());
        return ErrorResponse.builder()
                .code("BOOK_NOT_FOUND")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(((ServletWebRequest) request).getRequest().getRequestURI())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        return ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed")
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(((ServletWebRequest) request).getRequest().getRequestURI())
                .build();
    }
}
```

## Hata Kodları / Error Codes

### Türkçe
1. **Kitap Hataları**:
   - BOOK_NOT_FOUND: Kitap bulunamadı
   - BOOK_ALREADY_EXISTS: Kitap zaten mevcut
   - BOOK_NOT_AVAILABLE: Kitap müsait değil

2. **Kullanıcı Hataları**:
   - USER_NOT_FOUND: Kullanıcı bulunamadı
   - USER_ALREADY_EXISTS: Kullanıcı zaten mevcut
   - UNAUTHORIZED: Yetkisiz erişim

3. **Ödünç Alma Hataları**:
   - LOAN_NOT_FOUND: Ödünç kaydı bulunamadı
   - LOAN_ALREADY_EXISTS: Ödünç kaydı zaten mevcut
   - LOAN_OVERDUE: Gecikmiş ödünç

### English
1. **Book Errors**:
   - BOOK_NOT_FOUND: Book not found
   - BOOK_ALREADY_EXISTS: Book already exists
   - BOOK_NOT_AVAILABLE: Book not available

2. **User Errors**:
   - USER_NOT_FOUND: User not found
   - USER_ALREADY_EXISTS: User already exists
   - UNAUTHORIZED: Unauthorized access

3. **Loan Errors**:
   - LOAN_NOT_FOUND: Loan record not found
   - LOAN_ALREADY_EXISTS: Loan record already exists
   - LOAN_OVERDUE: Overdue loan

## Best Practices

### Türkçe
1. **Hata Mesajları**:
   - Kullanıcı dostu mesajlar
   - Teknik detaylar ayrı tutulur
   - Tutarlı mesaj formatı

2. **Hata Loglama**:
   - Detaylı log kayıtları
   - Hata izleme
   - Performans etkisi

3. **Güvenlik**:
   - Hassas bilgi gizleme
   - Hata detayları kontrolü
   - Güvenlik açığı önleme

### English
1. **Error Messages**:
   - User-friendly messages
   - Technical details separated
   - Consistent message format

2. **Error Logging**:
   - Detailed log records
   - Error tracking
   - Performance impact

3. **Security**:
   - Sensitive information hiding
   - Error details control
   - Security vulnerability prevention

## Örnek Kullanım / Example Usage

### Türkçe
#### 1. Hata Fırlatma
```java
@GetMapping("/books/{id}")
public BookResponseDto getBook(@PathVariable Long id) {
    return bookService.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
}
```

#### 2. Hata Yakalama
```java
try {
    bookService.createBook(bookRequestDto);
} catch (BookAlreadyExistsException ex) {
    log.error("Failed to create book: {}", ex.getMessage());
    throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
}
```

### English
#### 1. Throwing Exceptions
```java
@GetMapping("/books/{id}")
public BookResponseDto getBook(@PathVariable Long id) {
    return bookService.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
}
```

#### 2. Catching Exceptions
```java
try {
    bookService.createBook(bookRequestDto);
} catch (BookAlreadyExistsException ex) {
    log.error("Failed to create book: {}", ex.getMessage());
    throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
}
```

## Gelecek Geliştirmeler / Future Improvements

### Türkçe
1. **Hata İzleme**:
   - Merkezi hata izleme
   - Otomatik uyarılar
   - Hata analizi

2. **Hata Raporlama**:
   - Detaylı hata raporları
   - Trend analizi
   - Performans metrikleri

3. **Gelişmiş Özellikler**:
   - Çoklu dil desteği
   - Özelleştirilebilir hata formatları
   - Hata önleme mekanizmaları

### English
1. **Error Monitoring**:
   - Centralized error monitoring
   - Automated alerts
   - Error analysis

2. **Error Reporting**:
   - Detailed error reports
   - Trend analysis
   - Performance metrics

3. **Advanced Features**:
   - Multi-language support
   - Customizable error formats
   - Error prevention mechanisms 