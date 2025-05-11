package org.pehlivan.mert.librarymanagementsystem.controller.authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.authentication.AuthenticationResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UnauthorizedRoleException;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.service.authentication.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("api/v1/auth")
@Tag(name = "Authentication", description = "Authentication management API")
@SecurityRequirement(name = "bearerAuth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user", responses = {@ApiResponse(responseCode = "201", description = "User registered successfully"), @ApiResponse(responseCode = "400", description = "Invalid input"), @ApiResponse(responseCode = "409", description = "User already exists"), @ApiResponse(responseCode = "403", description = "Unauthorized role")})
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRequestDto userRequestDto) {
        log.info("Register method in AuthenticationController is started with {}", userRequestDto);

        // Check if user is trying to register with LIBRARIAN role
        if (userRequestDto.getRoles() != null && userRequestDto.getRoles().contains(Role.LIBRARIAN)) {
            log.error("Unauthorized attempt to register with LIBRARIAN role");
            throw new UnauthorizedRoleException("Only authenticated LIBRARIAN users can create new LIBRARIAN accounts");
        }

        // Force READER role for public registration
        userRequestDto.setRoles(Collections.singletonList(Role.READER));

        UserResponseDto registeredUser = authenticationService.register(userRequestDto);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", responses = {@ApiResponse(responseCode = "200", description = "Login successful"), @ApiResponse(responseCode = "401", description = "Invalid credentials")})
    public ResponseEntity<AuthenticationResponseDto> login(@Valid @RequestBody AuthenticationRequestDto authenticationRequestDto) {
        log.info("Login method in AuthenticationController is started with {}", authenticationRequestDto);
        AuthenticationResponseDto response = authenticationService.login(authenticationRequestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}