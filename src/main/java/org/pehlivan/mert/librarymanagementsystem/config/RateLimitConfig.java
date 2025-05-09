package org.pehlivan.mert.librarymanagementsystem.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket createNewBucket() {
        // Her kullanıcı için 100 istek hakkı
        // Her 5 dakikada 100 token yenilenir
        Bandwidth limit = Bandwidth.simple(100, Duration.ofMinutes(5));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
} 