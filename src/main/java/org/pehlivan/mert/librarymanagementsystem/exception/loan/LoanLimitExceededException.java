package org.pehlivan.mert.librarymanagementsystem.exception.loan;

public class LoanLimitExceededException extends RuntimeException {

    public LoanLimitExceededException(String message) {
        super(message);
    }
}
