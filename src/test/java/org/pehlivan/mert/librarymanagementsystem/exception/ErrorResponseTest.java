package org.pehlivan.mert.librarymanagementsystem.exception;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void testNoArgsConstructor() {
        ErrorResponse errorResponse = new ErrorResponse();
        
        assertNotNull(errorResponse);
        assertEquals(0, errorResponse.getStatus());
        assertNull(errorResponse.getMessage());
        assertNull(errorResponse.getTimestamp());
    }

    @Test
    void testAllArgsConstructor() {
        int status = 404;
        String message = "Not Found";
        LocalDateTime timestamp = LocalDateTime.now();
        
        ErrorResponse errorResponse = new ErrorResponse(status, message, timestamp);
        
        assertEquals(status, errorResponse.getStatus());
        assertEquals(message, errorResponse.getMessage());
        assertEquals(timestamp, errorResponse.getTimestamp());
    }

    @Test
    void testSettersAndGetters() {
        ErrorResponse errorResponse = new ErrorResponse();
        
        int status = 500;
        String message = "Internal Server Error";
        LocalDateTime timestamp = LocalDateTime.now();
        
        errorResponse.setStatus(status);
        errorResponse.setMessage(message);
        errorResponse.setTimestamp(timestamp);
        
        assertEquals(status, errorResponse.getStatus());
        assertEquals(message, errorResponse.getMessage());
        assertEquals(timestamp, errorResponse.getTimestamp());
    }
} 