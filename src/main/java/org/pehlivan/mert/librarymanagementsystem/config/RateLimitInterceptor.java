package org.pehlivan.mert.librarymanagementsystem.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pehlivan.mert.librarymanagementsystem.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Bucket bucket;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = request.getRemoteAddr();
        String requestURI = request.getRequestURI();
        
        log.info("Rate limit check for IP: {} and URI: {}", clientIp, requestURI);
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Reset", String.valueOf(probe.getNanosToWaitForReset() / 1_000_000_000));
            
            log.info("Request allowed. Remaining tokens: {}, Reset in: {} seconds", 
                    probe.getRemainingTokens(), 
                    probe.getNanosToWaitForReset() / 1_000_000_000);
            
            return true;
        }
        
        long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
        response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
        
        log.warn("Rate limit exceeded for IP: {} and URI: {}. Try again in {} seconds", 
                clientIp, requestURI, waitForRefill);
        
        throw new RateLimitExceededException(
                "Rate limit exceeded. Please try again in " + waitForRefill + " seconds.",
                waitForRefill,
                (int) probe.getRemainingTokens()
        );
    }
} 