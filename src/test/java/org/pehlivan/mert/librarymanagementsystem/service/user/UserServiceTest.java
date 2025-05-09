package org.pehlivan.mert.librarymanagementsystem.service.user;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MeterRegistry meterRegistry;
    @Mock private KafkaTemplate<String, UserRegistrationNotification> kafkaTemplate;
    @Mock private Counter totalUsersCounter;
    @Mock private Counter activeUsersCounter;
    @Mock private Counter newUsersCounter;
    @Mock private ModelMapper modelMapper;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;
    @Mock private SendResult<String, UserRegistrationNotification> sendResult;

    @InjectMocks private UserService userService;

    private User testUser;
    private UserRequestDto userRequestDto;
    private UserResponseDto userResponseDto;
    private UserUpdateRequestDto userUpdateRequestDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Collections.singletonList(Role.READER))
                .build();

        userRequestDto = UserRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .roles(Collections.singletonList(Role.READER))
                .build();

        userResponseDto = UserResponseDto.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        userUpdateRequestDto = UserUpdateRequestDto.builder()
                .username("updateduser")
                .email("updated@example.com")
                .build();

        when(modelMapper.map(any(UserRequestDto.class), eq(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDto.class))).thenReturn(userResponseDto);
        when(meterRegistry.counter("library.users.total")).thenReturn(totalUsersCounter);
        when(meterRegistry.counter("library.users.active")).thenReturn(activeUsersCounter);
        when(meterRegistry.counter("library.users.new")).thenReturn(newUsersCounter);

        // Setup security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
        when(authentication.isAuthenticated()).thenReturn(true);

        // Initialize counters
        userService.init();
    }

    @Test
    void createUser_Success() {
        when(userRepository.findByUsername(userRequestDto.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(userRequestDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(kafkaTemplate.send(anyString(), any(UserRegistrationNotification.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        UserResponseDto response = userService.createUser(userRequestDto);

        assertNotNull(response);
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
        verify(totalUsersCounter).increment();
        verify(activeUsersCounter).increment();
        verify(newUsersCounter).increment();
    }

    @Test
    void createUser_UserAlreadyExists() {
        when(userRepository.findByUsername(userRequestDto.getUsername())).thenReturn(Optional.of(testUser));

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(userRequestDto));
        verify(userRepository, never()).save(any(User.class));
        verify(totalUsersCounter, never()).increment();
        verify(activeUsersCounter, never()).increment();
        verify(newUsersCounter, never()).increment();
    }

    @Test
    void getAllUsers_Success() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        List<UserResponseDto> response = userService.getAllUsers();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testUser.getEmail(), response.get(0).getEmail());
        verify(userRepository).findAll();
    }

    @Test
    void getUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserResponseDto response = userService.getUser(1L);

        assertNotNull(response);
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getUsername(), response.getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUser_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser(1L));
        verify(userRepository).findById(1L);
    }

    @Test
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponseDto response = userService.updateUser(1L, userRequestDto);

        assertNotNull(response);
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        userService.deleteUser(1L);

        verify(userRepository).delete(testUser);
        verify(totalUsersCounter).increment(-1);
        verify(activeUsersCounter).increment(-1);
    }

    @Test
    void findByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<UserResponseDto> response = userService.findByEmail("test@example.com");

        assertTrue(response.isPresent());
        assertEquals(testUser.getEmail(), response.get().getEmail());
        assertEquals(testUser.getUsername(), response.get().getUsername());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void isCurrentUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authentication.getName()).thenReturn("testuser");

        boolean result = userService.isCurrentUser(1L);

        assertTrue(result);
        verify(userRepository).findById(1L);
    }

    @Test
    void isCurrentUserByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(authentication.getName()).thenReturn("testuser");

        boolean result = userService.isCurrentUserByEmail("test@example.com");

        assertTrue(result);
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void updateUserSelf_Success() {
        User updatedUser = User.builder()
                .id(1L)
                .username("updateduser")
                .email("updated@example.com")
                .password("encodedPassword")
                .roles(Collections.singletonList(Role.READER))
                .build();

        UserResponseDto updatedResponseDto = UserResponseDto.builder()
                .id(1L)
                .username("updateduser")
                .email("updated@example.com")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(modelMapper.map(updatedUser, UserResponseDto.class)).thenReturn(updatedResponseDto);

        UserResponseDto response = userService.updateUserSelf(1L, userUpdateRequestDto);

        assertNotNull(response);
        assertEquals("updated@example.com", response.getEmail());
        assertEquals("updateduser", response.getUsername());
        verify(userRepository).save(any(User.class));
    }
} 