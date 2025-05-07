package org.pehlivan.mert.librarymanagementsystem.exception.loan;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(String message) {
        super(message);
    }
}