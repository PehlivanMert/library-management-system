package org.pehlivan.mert.librarymanagementsystem.service.authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pehlivan.mert.librarymanagementsystem.exception.authentication.RefreshTokenException;
import org.pehlivan.mert.librarymanagementsystem.model.authentication.RefreshToken;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.authentication.RefreshTokenRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 604800000L); // 7 days

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .roles(List.of(Role.READER))
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token("test-refresh-token")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
    }

    @Test
    void createRefreshToken_Success() {
        doNothing().when(refreshTokenRepository).deleteByUser_Id(anyLong());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertFalse(result.isRevoked());
        verify(refreshTokenRepository).deleteByUser_Id(testUser.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void findByToken_Success() {
        when(refreshTokenRepository.findByToken("test-token")).thenReturn(Optional.of(testRefreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken("test-token");

        assertTrue(result.isPresent());
        assertEquals(testRefreshToken, result.get());
    }

    @Test
    void findByToken_NotFound() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.findByToken("invalid-token");

        assertFalse(result.isPresent());
    }

    @Test
    void verifyExpiration_ValidToken() {
        RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token("valid-token")
                .expiryDate(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();

        RefreshToken result = refreshTokenService.verifyExpiration(validToken);

        assertEquals(validToken, result);
    }

    @Test
    void verifyExpiration_ExpiredToken() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token("expired-token")
                .expiryDate(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        doNothing().when(refreshTokenRepository).delete(expiredToken);

        assertThrows(RefreshTokenException.class, () -> {
            refreshTokenService.verifyExpiration(expiredToken);
        });

        verify(refreshTokenRepository).delete(expiredToken);
    }

    @Test
    void verifyExpiration_RevokedToken() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token("revoked-token")
                .expiryDate(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();

        assertThrows(RefreshTokenException.class, () -> {
            refreshTokenService.verifyExpiration(revokedToken);
        });
    }

    @Test
    void deleteByUserId_Success() {
        refreshTokenService.deleteByUserId(1L);

        verify(refreshTokenRepository).deleteByUser_Id(1L);
    }

    @Test
    void revokeAllUserTokens_Success() {
        refreshTokenService.revokeAllUserTokens(1L);

        verify(refreshTokenRepository).revokeAllUserTokens(1L);
    }
}
