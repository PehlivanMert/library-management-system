package org.pehlivan.mert.librarymanagementsystem.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanLimitExceededException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotAvailableException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanAlreadyReturnedException;
import org.pehlivan.mert.librarymanagementsystem.service.loan.LoanService;
import org.pehlivan.mert.librarymanagementsystem.service.user.UserService;
import org.pehlivan.mert.librarymanagementsystem.service.book.BookService;
import org.pehlivan.mert.librarymanagementsystem.service.email.EmailService;
import org.pehlivan.mert.librarymanagementsystem.service.author.AuthorService;
import org.pehlivan.mert.librarymanagementsystem.dto.user.UserRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookRequestDto;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.model.loan.LoanStatus;
import org.pehlivan.mert.librarymanagementsystem.repository.loan.LoanRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.user.UserRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.book.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "management.health.mail.enabled=false",
    "spring.mail.enabled=false"
})
@ActiveProfiles("test")
@Transactional
public class LoanServiceIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookService bookService;

    @Autowired
    private AuthorService authorService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @MockBean
    private EmailService emailService;

    private LoanRequestDto loanRequestDto;
    private Long userId;
    private Long bookId;
    private Long authorId;
    private BookRequestDto bookRequestDto;

    @BeforeEach
    void setUp() {
        // Clean up all related data
        loanRepository.deleteAll();
        userRepository.deleteAll();
        bookRepository.deleteAll();

        // Create author first
        AuthorRequestDto authorRequestDto = AuthorRequestDto.builder()
                .name("Test")
                .surname("Author")
                .build();
        authorId = authorService.createAuthor(authorRequestDto).getId();

        // Create book request
        bookRequestDto = BookRequestDto.builder()
                .title("Test Book")
                .isbn("9780306406157")
                .stock(5)
                .pageCount(200)
                .publicationDate(new Date())
                .publisher("Test Publisher")
                .authorName("Test")
                .authorSurname("Author")
                .bookType(BookType.FICTION)
                .build();

        // Create book
        bookId = bookService.createBook(bookRequestDto).getId();

        // Create test user
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .roles(Collections.singletonList(Role.READER))
                .build();
        userId = userService.createUser(userRequestDto).getId();

        // Create loan request
        loanRequestDto = LoanRequestDto.builder()
                .userId(userId)
                .bookId(bookId)
                .borrowedDate(LocalDate.now())
                .build();

        // Mock email service
        doNothing().when(emailService).sendLoanNotification(
            anyString(),
            anyString(),
            anyString(),
            any(LocalDate.class),
            any(LocalDate.class)
        );
        doNothing().when(emailService).sendOverdueNotification(
            anyString(),
            anyString(),
            anyList()
        );
    }

    @Test
    void borrowBook_Success() {
        // When
        LoanResponseDto borrowedLoan = loanService.borrowBook(loanRequestDto);

        // Then
        assertNotNull(borrowedLoan);
        assertNotNull(borrowedLoan.getId());
        assertEquals(loanRequestDto.getUserId(), borrowedLoan.getUserId());
        assertEquals(loanRequestDto.getBookId(), borrowedLoan.getBookId());
        assertEquals(loanRequestDto.getBorrowedDate(), borrowedLoan.getBorrowedDate());
        assertNotNull(borrowedLoan.getDueDate());
        assertNull(borrowedLoan.getReturnDate());
        assertEquals(LoanStatus.BORROWED, borrowedLoan.getStatus());

        verify(emailService).sendLoanNotification(
            anyString(),
            anyString(),
            anyString(),
            any(LocalDate.class),
            any(LocalDate.class)
        );
    }

    @Test
    void borrowBook_BookNotAvailable() {
        // Given
        BookRequestDto bookRequestDto = BookRequestDto.builder()
                .title("Limited Book")
                .isbn("9783161484101")
                .stock(0)
                .pageCount(200)
                .publicationDate(new Date())
                .publisher("Test Publisher")
                .bookType(BookType.FICTION)
                .authorName("Test")
                .authorSurname("Author")
                .build();
        Long limitedBookId = bookService.createBook(bookRequestDto).getId();

        LoanRequestDto limitedBookRequest = LoanRequestDto.builder()
                .userId(userId)
                .bookId(limitedBookId)
                .borrowedDate(LocalDate.now())
                .build();

        // When & Then
        assertThrows(BookNotAvailableException.class, () -> loanService.borrowBook(limitedBookRequest));
        verify(emailService, never()).sendLoanNotification(
            anyString(),
            anyString(),
            anyString(),
            any(LocalDate.class),
            any(LocalDate.class)
        );
    }

    @Test
    void borrowBook_LoanLimitExceeded() {
        // Given
        // Create multiple books with unique ISBNs
        for (int i = 0; i < 3; i++) {
            BookRequestDto bookRequestDto = BookRequestDto.builder()
                    .title("Test Book " + i)
                    .isbn("978316148410" + (i + 2)) // Changed to use unique ISBNs
                    .stock(5)
                    .pageCount(200)
                    .publicationDate(new Date())
                    .publisher("Test Publisher")
                    .bookType(BookType.FICTION)
                    .authorName("Test")
                    .authorSurname("Author")
                    .build();
            Long newBookId = bookService.createBook(bookRequestDto).getId();

            LoanRequestDto newLoanRequest = LoanRequestDto.builder()
                    .userId(userId)
                    .bookId(newBookId)
                    .borrowedDate(LocalDate.now())
                    .build();

            loanService.borrowBook(newLoanRequest);
        }

        // When & Then
        assertThrows(LoanLimitExceededException.class, () -> loanService.borrowBook(loanRequestDto));
        verify(emailService, times(3)).sendLoanNotification(
            anyString(),
            anyString(),
            anyString(),
            any(LocalDate.class),
            any(LocalDate.class)
        );
    }

    @Test
    void getLoanById_NotFound() {
        // When & Then
        assertThrows(LoanNotFoundException.class, () -> loanService.getLoanById(999L));
    }

    @Test
    void returnBook_Success() {
        // Given
        LoanResponseDto borrowedLoan = loanService.borrowBook(loanRequestDto);

        // When
        LoanResponseDto returnedLoan = loanService.returnBook(borrowedLoan.getId());

        // Then
        assertNotNull(returnedLoan);
        assertEquals(borrowedLoan.getId(), returnedLoan.getId());
        assertEquals(userId, returnedLoan.getUserId());
        assertEquals(bookId, returnedLoan.getBookId());
        assertNotNull(returnedLoan.getReturnDate());
        assertEquals(LoanStatus.RETURNED, returnedLoan.getStatus());

        verify(emailService).sendLoanNotification(
            anyString(),
            anyString(),
            anyString(),
            any(LocalDate.class),
            any(LocalDate.class)
        );
    }

    @Test
    void returnBook_AlreadyReturned() {
        // Given
        LoanResponseDto borrowedLoan = loanService.borrowBook(loanRequestDto);
        loanService.returnBook(borrowedLoan.getId());

        // When & Then
        assertThrows(LoanAlreadyReturnedException.class, () -> loanService.returnBook(borrowedLoan.getId()));
        verify(emailService).sendLoanNotification(
            anyString(),
            anyString(),
            anyString(),
            any(LocalDate.class),
            any(LocalDate.class)
        );
    }

    @Test
    void getLoanHistoryByUser_Success() {
        // Given
        LoanResponseDto loan = loanService.borrowBook(loanRequestDto);

        // When
        List<LoanResponseDto> userLoans = loanService.getLoanHistoryByUser(userId);

        // Then
        assertNotNull(userLoans);
        assertFalse(userLoans.isEmpty());
        assertEquals(1, userLoans.size());
        assertEquals(loan.getId(), userLoans.get(0).getId());
        assertEquals(userId, userLoans.get(0).getUserId());

        verify(emailService).sendLoanNotification(
            anyString(),
            anyString(),
            anyString(),
            any(LocalDate.class),
            any(LocalDate.class)
        );
    }
}