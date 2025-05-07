package org.pehlivan.mert.librarymanagementsystem.exception.loan;

public class LoanAlreadyReturnedException extends RuntimeException {
    public LoanAlreadyReturnedException(String message) {
        super(message);
    }
} 