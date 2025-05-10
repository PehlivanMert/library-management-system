package org.pehlivan.mert.librarymanagementsystem.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UnauthorizedException;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.service.user.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    private UserRequestDto userRequestDto;
    private UserResponseDto userResponseDto;
    private AuthenticationRequestDto authenticationRequestDto;
    private AuthenticationResponseDto authenticationResponseDto;

    @BeforeEach
    void setUp() {
        userRequestDto = UserRequestDto.builder()
                .name("Test User")
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .roles(Collections.singletonList(Role.READER))
                .build();

        userResponseDto = UserResponseDto.builder()
                .id(1L)
                .name("Test User")
                .username("testuser")
                .email("test@example.com")
                .build();

        authenticationRequestDto = AuthenticationRequestDto.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        authenticationResponseDto = AuthenticationResponseDto.builder()
                .token("test-token")
                .build();
    }

    @Test
    void register_ShouldReturnCreatedUser() throws Exception {
        when(authenticationService.register(any(UserRequestDto.class))).thenReturn(userResponseDto);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_ShouldReturnAuthenticationResponse() throws Exception {
        when(authenticationService.login(any(AuthenticationRequestDto.class))).thenReturn(authenticationResponseDto);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authenticationRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"));
    }

    @Test
    void register_WithLibrarianRole_ShouldReturnForbidden() throws Exception {
        userRequestDto.setRoles(Collections.singletonList(Role.LIBRARIAN));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        when(authenticationService.login(any(AuthenticationRequestDto.class)))
                .thenThrow(new UnauthorizedException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authenticationRequestDto)))
                .andExpect(status().isUnauthorized());
    }
} 