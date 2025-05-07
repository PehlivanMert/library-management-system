package org.pehlivan.mert.librarymanagementsystem.exception.book;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BookStockException extends RuntimeException {
    public BookStockException(String message) {
        super(message);
    }
} 