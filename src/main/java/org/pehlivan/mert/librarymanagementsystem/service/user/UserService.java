package org.pehlivan.mert.librarymanagementsystem.service.user;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;

import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRegistrationNotification;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserUpdateRequestDto;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UserAlreadyExistsException;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UserNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.user.UserRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "user", cacheManager = "redisCacheManager")
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, UserRegistrationNotification> kafkaTemplate;

    private Counter totalUsersCounter;
    private Counter activeUsersCounter;
    private Counter newUsersCounter;

    @PostConstruct
    public void init() {
        totalUsersCounter = meterRegistry.counter("library.users.total");
        activeUsersCounter = meterRegistry.counter("library.users.active");
        newUsersCounter = meterRegistry.counter("library.users.new");
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        log.info("Entering createUser method for user: {}", userRequestDto.getUsername());
        User user = modelMapper.map(userRequestDto, User.class);
        user.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        if (userRepository.findByUsername(userRequestDto.getUsername()).isPresent()) {
            log.error("User already exists with username: {}", userRequestDto.getUsername());
            throw new UserAlreadyExistsException("User already exists with username: " + userRequestDto.getUsername());
        }

        if (userRequestDto.getRoles() == null || userRequestDto.getRoles().isEmpty()) {
            user.setRoles(Collections.singletonList(Role.READER));
        }

        try {
            User createdUser = userRepository.save(user);
            log.info("User created successfully with id: {} and roles: {}", createdUser.getId(), createdUser.getRoles());

            totalUsersCounter.increment();
            activeUsersCounter.increment();
            newUsersCounter.increment();

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
        } catch (Exception e) {
            log.error("An unexpected error occurred in createUser method", e);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'all'", unless = "#result.isEmpty()")
    public List<UserResponseDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponseDto> userResponseDtos = users.stream()
                .map(user -> modelMapper.map(user, UserResponseDto.class))
                .collect(Collectors.toList());
        log.info("getAllUsers method finished successfully. Total users found: {}", users.size());
        return userResponseDtos;
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'id:' + #id", unless = "#result == null")
    public UserResponseDto getUser(Long id) {
        log.info("Entering getUser method for id: {}", id);
        User user = userRepository.findById(id).orElseThrow(
                () -> {
                    log.error("User not found with id: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });
        log.info("getUserById method finished successfully for id: {}", id);
        return modelMapper.map(user, UserResponseDto.class);
    }

    @Transactional
    @CacheEvict(key = "{'id:' + #id, 'all'}")
    public UserResponseDto updateUser(Long id, UserRequestDto userRequestDto) {
        log.info("Entering updateUser method for id: {}", id);
        User existingUser = userRepository.findById(id).orElseThrow(
                () -> {
                    log.error("User not found with id: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });

        Optional.ofNullable(userRequestDto.getEmail()).ifPresent(existingUser::setEmail);
        Optional.ofNullable(userRequestDto.getName()).ifPresent(existingUser::setName);
        Optional.ofNullable(userRequestDto.getUsername()).ifPresent(existingUser::setUsername);
        Optional.ofNullable(userRequestDto.getRoles()).ifPresent(existingUser::setRoles);

        if (userRequestDto.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
        }

        User savedUser = userRepository.save(existingUser);
        log.info("User updated successfully with id: {}", id);
        return modelMapper.map(savedUser, UserResponseDto.class);
    }

    @Transactional
    @CacheEvict(key = "{'id:' + #id, 'all'}")
    public void deleteUser(Long id) {
        log.info("Entering deleteUser method for id: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> {
            log.error("User not found with id: {}", id);
            return new UserNotFoundException("User not found with id: " + id);
        });
        userRepository.delete(user);

        totalUsersCounter.increment(-1);
        activeUsersCounter.increment(-1);

        log.info("User deleted successfully with id: {}", id);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'email:' + #email", unless = "#result == null")
    public Optional<UserResponseDto> findByEmail(String email) {
        log.info("Entering findByEmail method for email: {}", email);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            log.info("User found with email: {}", email);
        } else {
            log.warn("No user found with email: {}", email);
        }
        return user.map(u -> modelMapper.map(u, UserResponseDto.class));
    }

    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found");
            return false;
        }

        String currentUsername = authentication.getName();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", userId);
                    return new UserNotFoundException("User not found with id: " + userId);
                });

        boolean isCurrentUser = user.getUsername().equals(currentUsername);
        log.info("User {} is{} the current user", userId, isCurrentUser ? "" : " not");
        return isCurrentUser;
    }

    public boolean isCurrentUserByEmail(String email) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found");
            return false;
        }

        String currentUsername = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UserNotFoundException("User not found with email: " + email);
                });

        boolean isCurrentUser = user.getUsername().equals(currentUsername);
        log.info("User with email {} is{} the current user", email, isCurrentUser ? "" : " not");
        return isCurrentUser;
    }

    public User getUserEntity(Long id) {
        log.info("Getting user entity for id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });
        log.info("User entity found for id: {}", id);
        return user;
    }

    public User getUserByUsername(String username) {
        log.info("Getting user entity for username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username: " + username);
                });
        log.info("User entity found for username: {}", username);
        return user;
    }

    @Transactional
    @CacheEvict(key = "{'id:' + #id, 'all'}")
    public UserResponseDto updateUserSelf(Long id, UserUpdateRequestDto userUpdateRequestDto) {
        log.info("Entering updateUserSelf method for id: {}", id);
        User existingUser = userRepository.findById(id).orElseThrow(
                () -> {
                    log.error("User not found with id: {}", id);
                    return new UserNotFoundException("User not found with id: " + id);
                });

        if (!existingUser.getRoles().contains(Role.READER)) {
            log.error("Access denied: User {} is not a reader", id);
            throw new AccessDeniedException("Only readers can update their own information");
        }

        Optional.ofNullable(userUpdateRequestDto.getEmail()).ifPresent(existingUser::setEmail);
        Optional.ofNullable(userUpdateRequestDto.getName()).ifPresent(existingUser::setName);
        Optional.ofNullable(userUpdateRequestDto.getUsername()).ifPresent(existingUser::setUsername);

        if (userUpdateRequestDto.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(userUpdateRequestDto.getPassword()));
        }

        User savedUser = userRepository.save(existingUser);
        log.info("User self-update completed successfully for id: {}", id);
        return modelMapper.map(savedUser, UserResponseDto.class);
    }
}