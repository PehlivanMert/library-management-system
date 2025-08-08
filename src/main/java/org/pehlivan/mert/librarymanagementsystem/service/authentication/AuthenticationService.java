package org.pehlivan.mert.librarymanagementsystem.service.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.RefreshTokenRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRegistrationNotification;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.authentication.RefreshTokenException;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UnauthorizedException;
import org.pehlivan.mert.librarymanagementsystem.model.authentication.RefreshToken;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.user.UserRepository;
import org.pehlivan.mert.librarymanagementsystem.security.JwtHelper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "user", cacheManager = "redisCacheManager")
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtHelper jwtHelper;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<String, UserRegistrationNotification> kafkaTemplate;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public UserResponseDto register(UserRequestDto userRequestDto) {
        log.info("Entering register method for user: {}", userRequestDto.getEmail());

        // Check if user already exists
        if (userRepository.findByEmail(userRequestDto.getEmail()).isPresent()) {
            log.error("User registration failed: email already exists {}", userRequestDto.getEmail());
            throw new RuntimeException("User with this email already exists");
        }

        if (userRepository.findByUsername(userRequestDto.getUsername()).isPresent()) {
            log.error("User registration failed: username already exists {}", userRequestDto.getUsername());
            throw new RuntimeException("User with this username already exists");
        }

        // Create new user
        User user = User.builder()
                .username(userRequestDto.getUsername())
                .email(userRequestDto.getEmail())
                .password(passwordEncoder.encode(userRequestDto.getPassword()))
                .name(userRequestDto.getName())
                .roles(userRequestDto.getRoles())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Send notification via Kafka
        UserRegistrationNotification notification = UserRegistrationNotification.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .build();

        kafkaTemplate.send("user-registration", notification);
        log.info("User registration notification sent to Kafka for user: {}", savedUser.getEmail());

        return modelMapper.map(savedUser, UserResponseDto.class);
    }

    public AuthenticationResponseDto login(AuthenticationRequestDto authRequestDto) {
        log.info("Entering login method for user: {}", authRequestDto.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequestDto.getEmail(),
                        authRequestDto.getPassword())
        );

        if (!authentication.isAuthenticated()) {
            log.error("Login failed for user: {}", authRequestDto.getEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        // Token üretimi
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtHelper.generateAccessToken(userDetails);
        
        // Refresh token üretimi
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        
        log.info("User {} logged in successfully", authRequestDto.getEmail());

        // DTO'ya sar ve dön
        return AuthenticationResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(3600000L) // 1 saat
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    public AuthenticationResponseDto refreshToken(RefreshTokenRequestDto refreshTokenRequestDto) {
        log.info("Refreshing token");
        
        // Refresh token'ı veritabanından bul
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenRequestDto.getRefreshToken())
                .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));
        
        // Refresh token'ın geçerliliğini kontrol et
        refreshTokenService.verifyExpiration(refreshToken);
        
        // Refresh token UUID formatında olduğu için JWT kontrolü yapmıyoruz
        // Refresh token'ın geçerliliği veritabanından kontrol ediliyor
        
        // Kullanıcıyı bul
        User user = refreshToken.getUser();
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> "ROLE_" + role.name())
                        .toArray(String[]::new))
                .build();
        
        // Yeni access token üret
        String newAccessToken = jwtHelper.generateAccessToken(userDetails);
        
        // Yeni refresh token üret (eski refresh token'ı sil)
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
        
        log.info("Token refreshed successfully for user: {}", user.getEmail());
        
        return AuthenticationResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(3600000L) // 1 saat
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public void logout(RefreshTokenRequestDto refreshTokenRequestDto) {
        log.info("User logout");
        
        // Refresh token'ı bul ve sil
        refreshTokenService.findByToken(refreshTokenRequestDto.getRefreshToken())
                .ifPresent(refreshToken -> {
                    refreshTokenService.deleteByUserId(refreshToken.getUser().getId());
                    log.info("User {} logged out successfully", refreshToken.getUser().getEmail());
                });
    }
}
