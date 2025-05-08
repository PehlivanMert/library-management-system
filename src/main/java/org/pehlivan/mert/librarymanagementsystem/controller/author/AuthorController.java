package org.pehlivan.mert.librarymanagementsystem.controller.author;

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
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorRequestDto;
import org.pehlivan.mert.librarymanagementsystem.service.author.AuthorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Author", description = "Author management APIs")
@RestController
@RequestMapping("api/v1/authors")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuthorController {

    private final AuthorService authorService;

    @Operation(summary = "Create a new author", description = "Creates a new author in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Author created successfully",
                    content = @Content(schema = @Schema(implementation = AuthorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<AuthorResponseDto> createAuthor(@Valid @RequestBody AuthorRequestDto authorRequestDto) {
        log.info("Creating new author: {}", authorRequestDto);
        return new ResponseEntity<>(authorService.createAuthor(authorRequestDto), HttpStatus.CREATED);
    }

    @Operation(summary = "Get all authors", description = "Retrieves all authors in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authors retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AuthorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "No authors found")
    })
    @GetMapping
    public ResponseEntity<List<AuthorResponseDto>> getAllAuthors() {
        log.info("Getting all authors");
        return ResponseEntity.ok(authorService.getAllAuthors());
    }

    @Operation(summary = "Get author by ID", description = "Retrieves a specific author by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AuthorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponseDto> getAuthorById(@PathVariable Long id) {
        log.info("Getting author by id: {}", id);
        return ResponseEntity.ok(authorService.getAuthor(id));
    }

    @Operation(summary = "Update an author", description = "Updates an existing author's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author updated successfully",
                    content = @Content(schema = @Schema(implementation = AuthorResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Author not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<AuthorResponseDto> updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorRequestDto authorRequestDto) {
        log.info("Updating author with id {}: {}", id, authorRequestDto);
        return ResponseEntity.ok(authorService.updateAuthor(id, authorRequestDto));
    }

    @Operation(summary = "Delete an author", description = "Deletes an author from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Author deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Author not found"),
            @ApiResponse(responseCode = "409", description = "Author has associated books")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        log.info("Deleting author with id: {}", id);
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }
} 