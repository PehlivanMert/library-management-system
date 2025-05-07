package org.pehlivan.mert.librarymanagementsystem.controller.book;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pehlivan.mert.librarymanagementsystem.event.BookAvailabilityEvent;
import org.pehlivan.mert.librarymanagementsystem.service.book.BookAvailabilityService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/v1/books/availability")
@RequiredArgsConstructor
@Tag(name = "Book Availability", description = "Real-time book availability APIs")
@SecurityRequirement(name = "bearerAuth")
public class BookAvailabilityController {

    private final BookAvailabilityService bookAvailabilityService;

    @Operation(summary = "Stream all book availability changes", 
              description = "Get real-time updates for all book availability changes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stream started successfully",
                    content = @Content(schema = @Schema(implementation = BookAvailabilityEvent.class))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'READER')")
    public Flux<BookAvailabilityEvent> streamAllBookAvailability() {
        log.info("Starting stream for all book availability changes");
        return bookAvailabilityService.getAvailabilityStream();
    }

    @Operation(summary = "Stream book availability changes for a specific book", 
              description = "Get real-time updates for a specific book's availability")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stream started successfully",
                    content = @Content(schema = @Schema(implementation = BookAvailabilityEvent.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping(value = "/{bookId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'READER')")
    public Flux<BookAvailabilityEvent> streamBookAvailability(@PathVariable Long bookId) {
        log.info("Starting stream for book availability changes for book: {}", bookId);
        return bookAvailabilityService.getAvailabilityStreamForBook(bookId);
    }
} 