package org.pehlivan.mert.librarymanagementsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:pass}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(redisPassword);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Default cache configuration (15 minutes TTL)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Create cache configurations for different services
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Book service caches - different TTLs
        cacheConfigurations.put("books", defaultConfig.entryTtl(Duration.ofHours(1))); // 1 saat
        cacheConfigurations.put("book", defaultConfig.entryTtl(Duration.ofMinutes(30))); // 30 dakika
        cacheConfigurations.put("bookSearch", defaultConfig.entryTtl(Duration.ofMinutes(10))); // 10 dakika

        // User service caches - different TTLs
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(2))); // 2 saat
        cacheConfigurations.put("user", defaultConfig.entryTtl(Duration.ofHours(1))); // 1 saat
        cacheConfigurations.put("userSearch", defaultConfig.entryTtl(Duration.ofMinutes(15))); // 15 dakika

        // Author service caches - different TTLs
        cacheConfigurations.put("authors", defaultConfig.entryTtl(Duration.ofHours(4))); // 4 saat
        cacheConfigurations.put("author", defaultConfig.entryTtl(Duration.ofHours(2))); // 2 saat
        cacheConfigurations.put("authorSearch", defaultConfig.entryTtl(Duration.ofMinutes(20))); // 20 dakika

        // Loan service caches - different TTLs
        cacheConfigurations.put("loans", defaultConfig.entryTtl(Duration.ofMinutes(30))); // 30 dakika
        cacheConfigurations.put("loan", defaultConfig.entryTtl(Duration.ofMinutes(15))); // 15 dakika
        cacheConfigurations.put("overdueLoans", defaultConfig.entryTtl(Duration.ofMinutes(5))); // 5 dakika

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}