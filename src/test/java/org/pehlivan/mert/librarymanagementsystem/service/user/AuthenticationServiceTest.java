package org.pehlivan.mert.librarymanagementsystem.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.RefreshTokenRequestDto;
import org.pehlivan.mert.librarymanagementsystem.exception.authentication.RefreshTokenException;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UnauthorizedException;
import org.pehlivan.mert.librarymanagementsystem.model.authentication.RefreshToken;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.user.UserRepository;
import org.pehlivan.mert.librarymanagementsystem.security.JwtHelper;
import org.pehlivan.mert.librarymanagementsystem.service.authentication.AuthenticationService;
import org.pehlivan.mert.librarymanagementsystem.service.authentication.RefreshTokenService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtHelper jwtHelper;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private Authentication authentication;
    @Mock private UserDetails userDetails;
    @Mock private ModelMapper modelMapper;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthenticationService authenticationService;

    private User testUser;
    private AuthenticationRequestDto authRequestDto;
    private RefreshTokenRequestDto refreshTokenRequestDto;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Collections.singletonList(Role.READER))
                .build();

        authRequestDto = AuthenticationRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        refreshTokenRequestDto = RefreshTokenRequestDto.builder()
                .refreshToken("test-refresh-token")
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .user(testUser)
                .token("test-refresh-token")
                .revoked(false)
                .build();
    }

    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(jwtHelper.generateAccessToken(userDetails)).thenReturn("test.jwt.token");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(testRefreshToken);

        AuthenticationResponseDto response = authenticationService.login(authRequestDto);

        assertNotNull(response);
        assertEquals("test.jwt.token", response.getAccessToken());
        assertEquals("test-refresh-token", response.getRefreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtHelper).generateAccessToken(userDetails);
        verify(refreshTokenService).createRefreshToken(testUser);
    }

    @Test
    void login_Failure() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authenticationService.login(authRequestDto));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtHelper, never()).generateAccessToken(any(UserDetails.class));
    }

    @Test
    void refreshToken_Success() {
        when(refreshTokenService.findByToken("test-refresh-token")).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenService.verifyExpiration(testRefreshToken)).thenReturn(testRefreshToken);
        when(jwtHelper.isRefreshToken("test-refresh-token")).thenReturn(true);
        when(jwtHelper.generateAccessToken(any(UserDetails.class))).thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(testRefreshToken);

        AuthenticationResponseDto response = authenticationService.refreshToken(refreshTokenRequestDto);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        verify(refreshTokenService).findByToken("test-refresh-token");
        verify(refreshTokenService).verifyExpiration(testRefreshToken);
        verify(jwtHelper).isRefreshToken("test-refresh-token");
    }

    @Test
    void refreshToken_InvalidToken() {
        when(refreshTokenService.findByToken("test-refresh-token")).thenReturn(Optional.empty());

        assertThrows(RefreshTokenException.class, () -> authenticationService.refreshToken(refreshTokenRequestDto));
        verify(refreshTokenService).findByToken("test-refresh-token");
    }

    @Test
    void logout_Success() {
        when(refreshTokenService.findByToken("test-refresh-token")).thenReturn(Optional.of(testRefreshToken));

        assertDoesNotThrow(() -> authenticationService.logout(refreshTokenRequestDto));
        verify(refreshTokenService).findByToken("test-refresh-token");
        verify(refreshTokenService).deleteByUserId(testUser.getId());
    }
} 