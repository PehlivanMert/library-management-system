package org.pehlivan.mert.librarymanagementsystem.controller.loan;

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
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.OverdueLoanReportResponseDto;
import org.pehlivan.mert.librarymanagementsystem.service.loan.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Loan", description = "Loan management APIs")
@RestController
@RequestMapping("api/v1/loans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final LoanService loanService;

    @Operation(summary = "Borrow a book", description = "Creates a new loan for a book")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book borrowed successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User or book not found"),
            @ApiResponse(responseCode = "409", description = "Book not available or loan limit exceeded")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'READER')")
    public ResponseEntity<LoanResponseDto> borrowBook(@Valid @RequestBody LoanRequestDto loanRequestDto) {
        log.info("Borrowing book with request: {}", loanRequestDto);
        return new ResponseEntity<>(loanService.borrowBook(loanRequestDto), HttpStatus.CREATED);
    }

    @Operation(summary = "Return a book", description = "Returns a borrowed book")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book returned successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "409", description = "Book already returned")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'READER')")
    public ResponseEntity<LoanResponseDto> returnBook(@PathVariable Long id) {
        log.info("Returning book for loan: {}", id);
        return ResponseEntity.ok(loanService.returnBook(id));
    }

    @Operation(summary = "Get loan history by user", description = "Retrieves loan history for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found or no loan history"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/history/user/{userId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'READER')")
    public ResponseEntity<List<LoanResponseDto>> getLoanHistoryByUser(@PathVariable Long userId) {
        log.info("Getting loan history for user: {}", userId);
        return ResponseEntity.ok(loanService.getLoanHistoryByUser(userId));
    }

    @Operation(summary = "Get all loan history", description = "Retrieves loan history for all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "No loan history found")
    })
    @GetMapping("/history")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<List<LoanResponseDto>> getAllLoanHistory() {
        log.info("Getting all loan history");
        return ResponseEntity.ok(loanService.getAllLoanHistory());
    }

    @Operation(summary = "Get late loans", description = "Retrieves all late loans")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Late loans retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "No late loans found")
    })
    @GetMapping("/late")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<List<LoanResponseDto>> getLateLoans() {
        log.info("Getting late loans");
        return ResponseEntity.ok(loanService.getLateLoans());
    }

    @Operation(summary = "Get loan by ID", description = "Retrieves a specific loan by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'READER')")
    public ResponseEntity<LoanResponseDto> getLoanById(@PathVariable Long id) {
        log.info("Getting loan by id: {}", id);
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    @Operation(summary = "Get overdue loan report", description = "Retrieves a detailed report of overdue loans")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overdue loan report retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OverdueLoanReportResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "No overdue loans found")
    })
    @GetMapping("/report/overdue")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<OverdueLoanReportResponseDto> getOverdueLoanReport() {
        log.info("Getting overdue loan report");
        return ResponseEntity.ok(loanService.getOverdueLoanReport());
    }
}