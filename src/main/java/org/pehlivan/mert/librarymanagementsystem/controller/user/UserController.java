package org.pehlivan.mert.librarymanagementsystem.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserUpdateRequestDto;
import org.pehlivan.mert.librarymanagementsystem.service.user.UserService;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserResponseDto;

import org.pehlivan.mert.librarymanagementsystem.exception.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "User", description = "User management APIs")
@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a new user", description = "Creates a new user in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        log.info("Creating new user with username: {}", userRequestDto.getUsername());
        try {
            UserResponseDto createdUser = userService.createUser(userRequestDto);
            log.info("User created successfully with username: {}", userRequestDto.getUsername());
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Failed to create user with username: {}", userRequestDto.getUsername(), e);
            throw e;
        }
    }

    @Operation(summary = "Get all users", description = "Retrieves all users in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "No users found")
    })
    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        log.info("Retrieving all users");
        try {
            List<UserResponseDto> users = userService.getAllUsers();
            log.info("Successfully retrieved {} users", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Failed to retrieve users", e);
            throw e;
        }
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content())
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN')")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        log.info("Retrieving user with id: {}", id);
        try {
            UserResponseDto user = userService.getUser(id);
            log.info("Successfully retrieved user with id: {}", id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Failed to retrieve user with id: {}", id, e);
            throw e;
        }
    }
    

    @Operation(summary = "Update a user", description = "Updates an existing user's information (Librarian only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequestDto userRequestDto) {
        log.info("Librarian updating user with id: {}", id);
        try {
            UserResponseDto updatedUser = userService.updateUser(id, userRequestDto);
            log.info("Successfully updated user with id: {}", id);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            log.error("Failed to update user with id: {}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Update own information", description = "Updates the authenticated user's own information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/self")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<UserResponseDto> updateUserSelf(@Valid @RequestBody UserUpdateRequestDto userUpdateRequestDto) {
        log.info("User attempting to update own information");
        try {
            Long userId = getAuthenticatedUserId();
            UserResponseDto updatedUser = userService.updateUserSelf(userId, userUpdateRequestDto);
            log.info("User successfully updated own information with id: {}", userId);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            log.error("Failed to update user's own information", e);
            throw e;
        }
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            try {
                return userService.findByEmail(userDetails.getUsername())
                        .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userDetails.getUsername()))
                        .getId();
            } catch (Exception e) {
                log.error("Failed to get authenticated user ID for email: {}", userDetails.getUsername(), e);
                throw e;
            }
        }
        log.error("No authenticated user found");
        throw new AccessDeniedException("User not authenticated");
    }

    @Operation(summary = "Delete a user", description = "Deletes a user from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "User has active loans")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Attempting to delete user with id: {}", id);
        try {
            userService.deleteUser(id);
            log.info("Successfully deleted user with id: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete user with id: {}", id, e);
            throw e;
        }
    }
}