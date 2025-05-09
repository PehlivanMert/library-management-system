package org.pehlivan.mert.librarymanagementsystem.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UserNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    private UserRequestDto userRequestDto;

    @BeforeEach
    void setUp() {
        userRequestDto = UserRequestDto.builder()
                .email("test@example.com")
                .username("testuser")
                .password("password123")
                .name("Test User")
                .roles(List.of(Role.READER))
                .build();
    }

    @Test
    void createUser_Success() {
        // When
        UserResponseDto createdUser = userService.createUser(userRequestDto);

        // Then
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals(userRequestDto.getEmail(), createdUser.getEmail());
        assertEquals(userRequestDto.getName(), createdUser.getName());
        assertEquals(userRequestDto.getUsername(), createdUser.getUsername());
    }

    @Test
    void getUser_Success() {
        // Given
        UserResponseDto createdUser = userService.createUser(userRequestDto);

        // When
        UserResponseDto foundUser = userService.getUser(createdUser.getId());

        // Then
        assertNotNull(foundUser);
        assertEquals(createdUser.getId(), foundUser.getId());
        assertEquals(createdUser.getEmail(), foundUser.getEmail());
        assertEquals(createdUser.getName(), foundUser.getName());
        assertEquals(createdUser.getUsername(), foundUser.getUsername());
    }

    @Test
    void getUser_NotFound() {
        // When & Then
        assertThrows(UserNotFoundException.class, () -> userService.getUser(999L));
    }

    @Test
    void updateUser_Success() {
        // Given
        UserResponseDto createdUser = userService.createUser(userRequestDto);
        UserRequestDto updateRequest = UserRequestDto.builder()
                .email("updated@example.com")
                .username("updateduser")
                .password("newpassword123")
                .name("Updated User")
                .build();

        // When
        UserResponseDto updatedUser = userService.updateUser(createdUser.getId(), updateRequest);

        // Then
        assertNotNull(updatedUser);
        assertEquals(createdUser.getId(), updatedUser.getId());
        assertEquals(updateRequest.getEmail(), updatedUser.getEmail());
        assertEquals(updateRequest.getName(), updatedUser.getName());
        assertEquals(updateRequest.getUsername(), updatedUser.getUsername());
    }

    @Test
    void deleteUser_Success() {
        // Given
        UserResponseDto createdUser = userService.createUser(userRequestDto);

        // When
        userService.deleteUser(createdUser.getId());

        // Then
        assertThrows(UserNotFoundException.class, () -> userService.getUser(createdUser.getId()));
    }

    @Test
    void getAllUsers_Success() {
        // Given
        userService.createUser(userRequestDto);
        userService.createUser(UserRequestDto.builder()
                .email("another@example.com")
                .username("anotheruser")
                .password("password123")
                .name("Another User")
                .roles(List.of(Role.READER))
                .build());

        // When
        var users = userService.getAllUsers();

        // Then
        assertNotNull(users);
        assertTrue(users.size() >= 2);
    }
}