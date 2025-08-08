package org.pehlivan.mert.librarymanagementsystem.service.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pehlivan.mert.librarymanagementsystem.exception.authentication.RefreshTokenException;
import org.pehlivan.mert.librarymanagementsystem.model.authentication.RefreshToken;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.authentication.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenDurationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        log.info("Creating refresh token for user: {}", user.getEmail());
        
        // Delete existing refresh token for this user
        refreshTokenRepository.deleteByUser_Id(user.getId());
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000))
                .revoked(false)
                .build();
        
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created successfully for user: {}", user.getEmail());
        
        return savedToken;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(LocalDateTime.now()) < 0) {
            log.warn("Refresh token expired for user: {}", token.getUser().getEmail());
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException("Refresh token was expired. Please make a new signin request");
        }
        
        if (token.isRevoked()) {
            log.warn("Refresh token revoked for user: {}", token.getUser().getEmail());
            throw new RefreshTokenException("Refresh token was revoked. Please make a new signin request");
        }
        
        return token;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        log.info("Deleting refresh tokens for user ID: {}", userId);
        refreshTokenRepository.deleteByUser_Id(userId);
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        log.info("Revoking all refresh tokens for user ID: {}", userId);
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    @Scheduled(cron = "0 0 2 * * *") // Her gün saat 2'de çalışır
    @Transactional
    public void deleteExpiredTokens() {
        log.info("Cleaning up expired refresh tokens");
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteExpiredTokens(now);
        log.info("Expired refresh tokens cleanup completed");
    }
}
