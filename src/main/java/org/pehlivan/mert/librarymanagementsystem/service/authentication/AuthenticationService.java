package org.pehlivan.mert.librarymanagementsystem.service.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.*;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UnauthorizedException;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UserAlreadyExistsException;
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
        String token = jwtHelper.generateToken(userDetails);
        log.info("User {} logged in successfully", authRequestDto.getEmail());

        // DTO'ya sar ve dön
        return AuthenticationResponseDto.builder()
                .token(token)
                .build();
    }

    @CacheEvict(allEntries = true)
    public UserResponseDto register(UserRequestDto userRequestDto) {
        log.info("Entering register method for user: {}", userRequestDto.getEmail());
        if (userRepository.findByEmail(userRequestDto.getEmail()).isPresent()) {
            log.error("User already exists: {}", userRequestDto.getEmail());
            throw new UserAlreadyExistsException("User already exists with email: " + userRequestDto.getEmail());
        }
        
        User user = modelMapper.map(userRequestDto, User.class);
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        
        User createdUser = userRepository.save(user);
        log.info("User {} registered successfully", userRequestDto.getEmail());

        // Send Kafka message for user registration
        try {
            log.info("=== Starting Kafka message sending process ===");
            log.info("Preparing Kafka message for user registration: {}", createdUser.getEmail());
            
            UserRegistrationNotification notification = new UserRegistrationNotification(
                createdUser.getEmail(),
                createdUser.getUsername()
            );
            
            log.info("Kafka message content: {}", notification);
            log.info("Sending Kafka message to user-registration topic");
            
            kafkaTemplate.send("user-registration", notification)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka message sent successfully for user: {}", createdUser.getEmail());
                            log.info("Message sent to partition: {}, offset: {}", 
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send Kafka message for user: {}", createdUser.getEmail(), ex);
                        }
                    });
            log.info("=== Kafka message sending process completed ===");
        } catch (Exception e) {
            log.error("Error sending Kafka message for user registration: {}", createdUser.getEmail(), e);
            // Don't throw the exception as the user is already saved
        }

        return modelMapper.map(createdUser, UserResponseDto.class);
    }
}
