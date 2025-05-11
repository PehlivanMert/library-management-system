package org.pehlivan.mert.librarymanagementsystem.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.exception.author.AuthorAlreadyExistException;
import org.pehlivan.mert.librarymanagementsystem.exception.author.AuthorNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.*;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.*;
import org.pehlivan.mert.librarymanagementsystem.exception.rate.RateLimitExceededException;
import org.pehlivan.mert.librarymanagementsystem.exception.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    // Author Exception Tests
    @Test
    void handleAuthorNotFoundException_ShouldReturnNotFoundStatus() {
        AuthorNotFoundException ex = new AuthorNotFoundException("Author not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthorNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Author not found", response.getBody().getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
    }

    @Test
    void handleAuthorAlreadyExistsException_ShouldReturnConflictStatus() {
        AuthorAlreadyExistException ex = new AuthorAlreadyExistException("Author already exists");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthorAlreadyExistsException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Author already exists", response.getBody().getMessage());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
    }

    // User Exception Tests
    @Test
    void handleUserNotFoundException_ShouldReturnNotFoundStatus() {
        UserNotFoundException ex = new UserNotFoundException("User not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
    }

    @Test
    void handleUserAlreadyExistsException_ShouldReturnConflictStatus() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("User already exists");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserAlreadyExistsException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("User already exists", response.getBody().getMessage());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
    }

    @Test
    void handleUnauthorizedRoleException_ShouldReturnForbiddenStatus() {
        UnauthorizedRoleException ex = new UnauthorizedRoleException("Unauthorized role");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedRoleException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Unauthorized role", response.getBody().getMessage());
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getBody().getStatus());
    }

    @Test
    void handleUnauthorizedException_ShouldReturnUnauthorizedStatus() {
        UnauthorizedException ex = new UnauthorizedException("Unauthorized access");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized access", response.getBody().getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().getStatus());
    }

    // Book Exception Tests
    @Test
    void handleBookNotFoundException_ShouldReturnNotFoundStatus() {
        BookNotFoundException ex = new BookNotFoundException("Book not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBookNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Book not found", response.getBody().getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
    }

    @Test
    void handleBookNotAvailableException_ShouldReturnBadRequestStatus() {
        BookNotAvailableException ex = new BookNotAvailableException("Book not available");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBookNotAvailableException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Book not available", response.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
    }

    @Test
    void handleBookAlreadyExistsException_ShouldReturnConflictStatus() {
        BookAlreadyExistsException ex = new BookAlreadyExistsException("Book already exists");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBookAlreadyExistsException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Book already exists", response.getBody().getMessage());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
    }

    @Test
    void handleBookStockException_ShouldReturnBadRequestStatus() {
        BookStockException ex = new BookStockException("Book stock error");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBookStockException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Book stock error", response.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
    }

    // Loan Exception Tests
    @Test
    void handleLoanNotFoundException_ShouldReturnNotFoundStatus() {
        LoanNotFoundException ex = new LoanNotFoundException("Loan not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleLoanNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Loan not found", response.getBody().getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
    }

    @Test
    void handleLoanLimitExceededException_ShouldReturnBadRequestStatus() {
        LoanLimitExceededException ex = new LoanLimitExceededException("Loan limit exceeded");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleLoanLimitExceededException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Loan limit exceeded", response.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
    }

    @Test
    void handleLoanAlreadyReturnedException_ShouldReturnBadRequestStatus() {
        LoanAlreadyReturnedException ex = new LoanAlreadyReturnedException("Loan already returned");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleLoanAlreadyReturnedException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Loan already returned", response.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
    }

    @Test
    void handleUserLoanHistoryNotFoundException_ShouldReturnNotFoundStatus() {
        UserLoanHistoryNotFoundException ex = new UserLoanHistoryNotFoundException("User loan history not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserLoanHistoryNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User loan history not found", response.getBody().getMessage());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
    }

    // Rate Limit Exception Test
    @Test
    void handleRateLimitExceededException_ShouldReturnTooManyRequestsStatus() {
        RateLimitExceededException ex = new RateLimitExceededException("Rate limit exceeded", 1000L, 0);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleRateLimitExceededException(ex);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("Rate limit exceeded", response.getBody().getMessage());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getBody().getStatus());
    }

    // Security Exception Tests
    @Test
    void handleAccessDeniedException_ShouldReturnForbiddenStatus() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDeniedException(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You don't have permission to perform this action", response.getBody().getMessage());
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getBody().getStatus());
    }

    @Test
    void handleBadCredentialsException_ShouldReturnUnauthorizedStatus() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentialsException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid username or password", response.getBody().getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().getStatus());
    }

    @Test
    void handleAuthenticationException_ShouldReturnUnauthorizedStatus() {
        AuthenticationException ex = new BadCredentialsException("Authentication failed");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthenticationException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Authentication failed", response.getBody().getMessage());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getBody().getStatus());
    }

    // Validation Exception Tests
    @Test
    void handleMethodArgumentNotValidException_ShouldReturnBadRequestStatus() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "default message");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<Map<String, String>> response = exceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().containsKey("field"));
        assertEquals("default message", response.getBody().get("field"));
    }

    @Test
    void handleMethodArgumentTypeMismatchException_ShouldReturnBadRequestStatus() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("param");
        when(ex.getRequiredType()).thenReturn((Class) String.class);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Parameter 'param' should be of type String"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
    }

    @Test
    void handleHttpMessageNotReadableException_ShouldReturnBadRequestStatus() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid request body");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid request body format", response.getBody().getMessage());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
    }

    // Global Exception Test
    @Test
    void handleGlobalException_ShouldReturnInternalServerErrorStatus() {
        Exception ex = new RuntimeException("Unexpected error");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Unexpected error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
    }
} 