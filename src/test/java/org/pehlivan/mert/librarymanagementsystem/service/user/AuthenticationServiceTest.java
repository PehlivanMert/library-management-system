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
import org.pehlivan.mert.librarymanagementsystem.dto.user.*;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UnauthorizedException;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UserAlreadyExistsException;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.user.UserRepository;
import org.pehlivan.mert.librarymanagementsystem.security.JwtHelper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    @Mock private KafkaTemplate<String, UserRegistrationNotification> kafkaTemplate;
    @Mock private Authentication authentication;
    @Mock private UserDetails userDetails;
    @Mock private SendResult<String, UserRegistrationNotification> sendResult;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private AuthenticationService authenticationService;

    private User testUser;
    private UserRequestDto userRequestDto;
    private AuthenticationRequestDto authRequestDto;
    private UserResponseDto userResponseDto;

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

        authRequestDto = AuthenticationRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        when(modelMapper.map(any(UserRequestDto.class), eq(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserResponseDto.class))).thenReturn(userResponseDto);
    }

    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtHelper.generateToken(userDetails)).thenReturn("test.jwt.token");

        AuthenticationResponseDto response = authenticationService.login(authRequestDto);

        assertNotNull(response);
        assertEquals("test.jwt.token", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtHelper).generateToken(userDetails);
    }

    @Test
    void login_Failure() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authenticationService.login(authRequestDto));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtHelper, never()).generateToken(any(UserDetails.class));
    }

    @Test
    void register_Success() {
        when(userRepository.findByEmail(userRequestDto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(userRequestDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(kafkaTemplate.send(anyString(), any(UserRegistrationNotification.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        UserResponseDto response = authenticationService.register(userRequestDto);

        assertNotNull(response);
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user-registration"), any(UserRegistrationNotification.class));
    }

    @Test
    void register_UserAlreadyExists() {
        when(userRepository.findByEmail(userRequestDto.getEmail())).thenReturn(Optional.of(testUser));

        assertThrows(UserAlreadyExistsException.class, () -> authenticationService.register(userRequestDto));
        verify(userRepository, never()).save(any(User.class));
        verify(kafkaTemplate, never()).send(anyString(), any(UserRegistrationNotification.class));
    }

    @Test
    void register_KafkaFailure() {
        when(userRepository.findByEmail(userRequestDto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(userRequestDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(kafkaTemplate.send(anyString(), any(UserRegistrationNotification.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

        UserResponseDto response = authenticationService.register(userRequestDto);

        assertNotNull(response);
        assertEquals(testUser.getEmail(), response.getEmail());
        assertEquals(testUser.getUsername(), response.getUsername());
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user-registration"), any(UserRegistrationNotification.class));
    }
} 