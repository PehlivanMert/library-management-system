package org.pehlivan.mert.librarymanagementsystem.exception.rate;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {
    
    private final long resetTime;
    private final int remainingRequests;
    
    public RateLimitExceededException(String message, long resetTime, int remainingRequests) {
        super(message);
        this.resetTime = resetTime;
        this.remainingRequests = remainingRequests;
    }

} 