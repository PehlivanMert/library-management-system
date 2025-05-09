package org.pehlivan.mert.librarymanagementsystem.service.loan;

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
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.UserLoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotAvailableException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanLimitExceededException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanAlreadyReturnedException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.UserLoanHistoryNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UserNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.pehlivan.mert.librarymanagementsystem.model.loan.Loan;
import org.pehlivan.mert.librarymanagementsystem.model.loan.LoanStatus;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.loan.LoanRepository;
import org.pehlivan.mert.librarymanagementsystem.service.book.BookService;
import org.pehlivan.mert.librarymanagementsystem.service.email.EmailService;
import org.pehlivan.mert.librarymanagementsystem.service.user.UserService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private BookService bookService;
    @Mock private UserService userService;
    @Mock private EmailService emailService;
    @Mock private MeterRegistry meterRegistry;
    @Mock private Counter loanCounter;
    @Mock private Counter overdueCounter;

    @InjectMocks private LoanService loanService;

    private User testUser;
    private Book testBook;
    private Loan testLoan;
    private LoanRequestDto loanRequestDto;

    private final LocalDate fixedDate = LocalDate.of(2023, 1, 1);

    @BeforeEach
    void setUp() {
        // Prepare a test user and book
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testBook = Book.builder()
                .id(1L)
                .title("Test Book")
                .availableCount(5)
                .stock(10)
                .build();

        // Prepare a test loan
        testLoan = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(fixedDate)
                .dueDate(fixedDate.plusDays(14))
                .status(LoanStatus.BORROWED)
                .build();

        // Prepare the DTO
        loanRequestDto = LoanRequestDto.builder()
                .userId(1L)
                .bookId(1L)
                .borrowedDate(fixedDate)
                .build();

        when(meterRegistry.counter("library.loans.total")).thenReturn(loanCounter);
        when(meterRegistry.counter("library.loans.overdue")).thenReturn(overdueCounter);
        loanService.init();
    }

    @Test
    void borrowBook_Success() {
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        LoanResponseDto response = loanService.borrowBook(loanRequestDto);

        assertNotNull(response);
        assertEquals(testLoan.getId(), response.getId());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testBook.getId(), response.getBookId());
        assertEquals(fixedDate, response.getBorrowedDate());
        assertEquals(fixedDate.plusDays(14), response.getDueDate());
        assertEquals(LoanStatus.BORROWED, response.getStatus());

        verify(bookService).decreaseAvailableCount(testBook.getId());
        verify(emailService).sendLoanNotification(
            eq(testUser.getEmail()),
            eq(testUser.getUsername()),
            eq(testBook.getTitle()),
            eq(fixedDate),
            eq(fixedDate.plusDays(14))
        );
        verify(loanCounter).increment();
    }

    @Test
    void borrowBook_BookNotAvailable() {
        testBook.setAvailableCount(0);
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);

        assertThrows(BookNotAvailableException.class, () -> loanService.borrowBook(loanRequestDto));
        verify(bookService, never()).decreaseAvailableCount(anyLong());
        verify(emailService, never()).sendLoanNotification(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void borrowBook_LoanLimitExceeded() {
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED)).thenReturn(3L);

        assertThrows(LoanLimitExceededException.class, () -> loanService.borrowBook(loanRequestDto));
        verify(bookService, never()).decreaseAvailableCount(anyLong());
        verify(emailService, never()).sendLoanNotification(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void returnBook_Success() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan updatedLoan = invocation.getArgument(0);
            updatedLoan.setStatus(LoanStatus.RETURNED);
            updatedLoan.setReturnDate(LocalDate.now());
            return updatedLoan;
        });

        LoanResponseDto response = loanService.returnBook(1L);

        assertNotNull(response);
        assertEquals(LoanStatus.RETURNED, response.getStatus());
        assertNotNull(response.getReturnDate());
        verify(bookService).increaseAvailableCount(testBook.getId());
    }

    @Test
    void returnBook_LoanNotFound() {
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(LoanNotFoundException.class, () -> loanService.returnBook(1L));
        verify(bookService, never()).increaseAvailableCount(anyLong());
    }

    @Test
    void returnBook_AlreadyReturned() {
        testLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        assertThrows(LoanAlreadyReturnedException.class, () -> loanService.returnBook(1L));
        verify(bookService, never()).increaseAvailableCount(anyLong());
    }

    @Test
    void getLoanById_Success() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));

        LoanResponseDto response = loanService.getLoanById(1L);

        assertNotNull(response);
        assertEquals(testLoan.getId(), response.getId());
        assertEquals(testUser.getId(), response.getUserId());
        assertEquals(testBook.getId(), response.getBookId());
    }

    @Test
    void getLoanById_NotFound() {
        when(loanRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(LoanNotFoundException.class, () -> loanService.getLoanById(1L));
    }

    @Test
    void borrowBookForUser_Success() {
        UserLoanRequestDto requestDto = UserLoanRequestDto.builder()
                .bookId(1L)
                .borrowedDate(fixedDate)
                .build();

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        LoanResponseDto response = loanService.borrowBookForUser(requestDto, 1L);

        assertNotNull(response);
        assertEquals(testLoan.getId(), response.getId());
        verify(bookService).decreaseAvailableCount(testBook.getId());
        verify(emailService).sendLoanNotification(
            eq(testUser.getEmail()),
            eq(testUser.getUsername()),
            eq(testBook.getTitle()),
            eq(fixedDate),
            eq(fixedDate.plusDays(14))
        );
        verify(loanCounter).increment();
    }

    @Test
    void checkAndUpdateOverdueLoans_UpdatesOverdueStatusAndSendsEmail() {
        LocalDate pastDueDate = LocalDate.now().minusDays(5);
        Loan overdueLoan = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDueDate.minusDays(14))
                .dueDate(pastDueDate)
                .status(LoanStatus.BORROWED)
                .build();

        when(loanRepository.findByStatusAndDueDateBefore(eq(LoanStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(List.of(overdueLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        loanService.checkAndUpdateOverdueLoans();

        verify(loanRepository).save(argThat(loan ->
                loan.getStatus() == LoanStatus.OVERDUE &&
                loan.getPenaltyAmount() == 5 * 5.0
        ));
        verify(emailService).sendOverdueNotification(
            eq(testUser.getEmail()),
            eq(testUser.getUsername()),
            anyList()
        );
        verify(overdueCounter).increment();
    }

    @Test
    void getLoanHistoryByUser_Success() {
        List<Loan> loans = Arrays.asList(testLoan);
        when(loanRepository.findByUser_Id(1L)).thenReturn(loans);

        List<LoanResponseDto> response = loanService.getLoanHistoryByUser(1L);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testLoan.getId(), response.get(0).getId());
        assertEquals(testUser.getId(), response.get(0).getUserId());
    }

    @Test
    void getLateLoans_Success() {
        LocalDate pastDueDate = LocalDate.now().minusDays(5);
        Loan overdueLoan = Loan.builder()
                .id(2L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDueDate.minusDays(14))
                .dueDate(pastDueDate)
                .status(LoanStatus.OVERDUE)
                .penaltyAmount(25.0)
                .build();

        List<Loan> loans = Arrays.asList(overdueLoan);
        when(loanRepository.findByStatus(LoanStatus.OVERDUE)).thenReturn(loans);

        List<LoanResponseDto> response = loanService.getLateLoans();

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(overdueLoan.getId(), response.get(0).getId());
        assertEquals(LoanStatus.OVERDUE, response.get(0).getStatus());
        assertEquals(25.0, response.get(0).getPenaltyAmount());
    }

    @Test
    void generateLoanReport_EmptyLoans() {
        // Arrange
        when(loanRepository.findAll()).thenReturn(List.of());
        when(loanRepository.findByStatus(LoanStatus.OVERDUE)).thenReturn(List.of());
        when(loanRepository.findByStatus(LoanStatus.BORROWED)).thenReturn(List.of());
        when(loanRepository.findByStatus(LoanStatus.RETURNED)).thenReturn(List.of());

        // Act & Assert
        assertThrows(LoanNotFoundException.class, () -> loanService.generateLoanReport());
    }

    @Test
    void generateLoanReport_Success() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate pastDate = today.minusDays(20);
        LocalDate futureDate = today.plusDays(5);
        
        // Overdue loan
        Loan overdueLoan = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDate)
                .dueDate(pastDate.plusDays(14))
                .status(LoanStatus.OVERDUE)
                .penaltyAmount(30.0)
                .build();

        // Upcoming due loan
        Loan upcomingLoan = Loan.builder()
                .id(2L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(today)
                .dueDate(futureDate)
                .status(LoanStatus.BORROWED)
                .build();

        // Returned loan
        Loan returnedLoan = Loan.builder()
                .id(3L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDate)
                .dueDate(pastDate.plusDays(14))
                .returnDate(pastDate.plusDays(15))
                .status(LoanStatus.RETURNED)
                .build();

        List<Loan> allLoans = Arrays.asList(overdueLoan, upcomingLoan, returnedLoan);
        when(loanRepository.findAll()).thenReturn(allLoans);
        when(loanRepository.findByStatus(LoanStatus.OVERDUE)).thenReturn(List.of(overdueLoan));
        when(loanRepository.findByStatus(LoanStatus.BORROWED)).thenReturn(List.of(upcomingLoan));
        when(loanRepository.findByStatus(LoanStatus.RETURNED)).thenReturn(List.of(returnedLoan));

        // Act
        String report = loanService.generateLoanReport();

        // Assert
        assertNotNull(report);
        assertTrue(report.contains("KÜTÜPHANE ÖDÜNÇ KİTAP RAPORU"));
        assertTrue(report.contains("Toplam Ödünç Kitap Sayısı: 3"));
        assertTrue(report.contains("Geciken Kitap Sayısı: 1"));
        assertTrue(report.contains("Yaklaşan Son Tarihli Kitap Sayısı: 1"));
        assertTrue(report.contains("İade Edilen Kitap Sayısı: 1"));
        assertTrue(report.contains("AKTİF ÖDÜNÇ KİTAPLAR"));
        assertTrue(report.contains("GECİKEN KİTAPLAR"));
        assertTrue(report.contains("SON İADE EDİLEN KİTAPLAR"));
    }

    @Test
    void borrowBook_WithCustomBorrowDate() {
        // Arrange
        LocalDate customDate = LocalDate.now().minusDays(5);
        loanRequestDto.setBorrowedDate(customDate);

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setBorrowedDate(customDate);
            savedLoan.setDueDate(customDate.plusDays(14));
            return savedLoan;
        });

        // Act
        LoanResponseDto response = loanService.borrowBook(loanRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(customDate, response.getBorrowedDate());
        assertEquals(customDate.plusDays(14), response.getDueDate());
    }

    @Test
    void returnBook_WithOverduePenalty() {
        // Arrange
        LocalDate borrowedDate = LocalDate.now().minusDays(20);
        LocalDate dueDate = borrowedDate.plusDays(14);
        LocalDate returnDate = LocalDate.now();

        testLoan.setBorrowedDate(borrowedDate);
        testLoan.setDueDate(dueDate);
        testLoan.setStatus(LoanStatus.OVERDUE);
        testLoan.setPenaltyAmount(30.0);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan updatedLoan = invocation.getArgument(0);
            updatedLoan.setStatus(LoanStatus.RETURNED);
            updatedLoan.setReturnDate(returnDate);
            return updatedLoan;
        });

        // Act
        LoanResponseDto response = loanService.returnBook(1L);

        // Assert
        assertNotNull(response);
        assertEquals(LoanStatus.RETURNED, response.getStatus());
        assertEquals(returnDate, response.getReturnDate());
        assertTrue(response.getPenaltyAmount() > 0);
    }

    @Test
    void getLoanHistoryByUser_NoHistory() {
        // Arrange
        when(userService.getUser(1L)).thenReturn(null);
        when(loanRepository.findByUser_Id(1L)).thenReturn(List.of());

        // Act & Assert
        assertThrows(UserLoanHistoryNotFoundException.class, () -> loanService.getLoanHistoryByUser(1L));
    }

    @Test
    void borrowBookForUser_WithInvalidUser() {
        // Arrange
        UserLoanRequestDto requestDto = UserLoanRequestDto.builder()
                .bookId(1L)
                .borrowedDate(fixedDate)
                .build();

        when(userService.getUserEntity(1L)).thenThrow(new UserNotFoundException("User not found"));

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> loanService.borrowBookForUser(requestDto, 1L));
    }

    @Test
    void checkAndUpdateOverdueLoans_NoOverdueLoans() {
        // Arrange
        when(loanRepository.findByStatusAndDueDateBefore(eq(LoanStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(List.of());

        // Act
        loanService.checkAndUpdateOverdueLoans();

        // Assert
        verify(loanRepository, never()).save(any(Loan.class));
        verify(emailService, never()).sendOverdueNotification(anyString(), anyString(), anyList());
        verify(overdueCounter, never()).increment();
    }

    @Test
    void checkAndUpdateOverdueLoans_ExceedsMaxEmailDays() {
        // Arrange
        LocalDate pastDueDate = LocalDate.now().minusDays(31);
        Loan overdueLoan = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDueDate.minusDays(14))
                .dueDate(pastDueDate)
                .status(LoanStatus.BORROWED)
                .build();

        when(loanRepository.findByStatusAndDueDateBefore(eq(LoanStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(List.of(overdueLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        loanService.checkAndUpdateOverdueLoans();

        // Assert
        verify(loanRepository).save(argThat(loan ->
                loan.getStatus() == LoanStatus.OVERDUE &&
                loan.getPenaltyAmount() == 31 * 5.0
        ));
        verify(emailService, never()).sendOverdueNotification(anyString(), anyString(), anyList());
        verify(overdueCounter).increment();
    }

    @Test
    void getAllLoanHistory_EmptyList() {
        // Arrange
        when(loanRepository.findAll()).thenReturn(List.of());

        // Act & Assert
        assertThrows(LoanNotFoundException.class, () -> loanService.getAllLoanHistory());
    }

    @Test
    void borrowBook_WithBookServiceFailure() {
        // Arrange
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        doThrow(new BookNotAvailableException("Book is not available"))
                .when(bookService).decreaseAvailableCount(anyLong());

        // Act & Assert
        assertThrows(BookNotAvailableException.class, () -> loanService.borrowBook(loanRequestDto));
        verify(loanRepository).delete(testLoan);
    }

    @Test
    void checkAndUpdateOverdueLoans_WithMultipleOverdueLoans() {
        // Arrange
        LocalDate pastDueDate = LocalDate.now().minusDays(5);
        LocalDate olderDueDate = LocalDate.now().minusDays(15);
        
        Loan overdueLoan1 = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDueDate.minusDays(14))
                .dueDate(pastDueDate)
                .status(LoanStatus.BORROWED)
                .build();

        Loan overdueLoan2 = Loan.builder()
                .id(2L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(olderDueDate.minusDays(14))
                .dueDate(olderDueDate)
                .status(LoanStatus.BORROWED)
                .build();

        when(loanRepository.findByStatusAndDueDateBefore(eq(LoanStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(Arrays.asList(overdueLoan1, overdueLoan2));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        loanService.checkAndUpdateOverdueLoans();

        // Assert
        verify(loanRepository, times(2)).save(argThat(loan ->
                loan.getStatus() == LoanStatus.OVERDUE &&
                loan.getPenaltyAmount() > 0
        ));
        verify(emailService, times(2)).sendOverdueNotification(
            eq(testUser.getEmail()),
            eq(testUser.getUsername()),
            anyList()
        );
        verify(overdueCounter, times(2)).increment();
    }

    @Test
    void borrowBook_WithCustomDueDate() {
        // Arrange
        LocalDate customBorrowDate = LocalDate.now().minusDays(5);
        LocalDate customDueDate = customBorrowDate.plusDays(21); // 3 hafta
        
        loanRequestDto.setBorrowedDate(customBorrowDate);

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setBorrowedDate(customBorrowDate);
            savedLoan.setDueDate(customDueDate);
            return savedLoan;
        });

        // Act
        LoanResponseDto response = loanService.borrowBook(loanRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(customBorrowDate, response.getBorrowedDate());
        assertEquals(customDueDate, response.getDueDate());
    }

    @Test
    void returnBook_WithPartialPenaltyPayment() {
        // Arrange
        LocalDate borrowedDate = LocalDate.now().minusDays(20);
        LocalDate dueDate = borrowedDate.plusDays(14);
        LocalDate returnDate = LocalDate.now();
        double partialPayment = 25.0;

        testLoan.setBorrowedDate(borrowedDate);
        testLoan.setDueDate(dueDate);
        testLoan.setStatus(LoanStatus.OVERDUE);
        testLoan.setPenaltyAmount(30.0);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan updatedLoan = invocation.getArgument(0);
            updatedLoan.setStatus(LoanStatus.RETURNED);
            updatedLoan.setReturnDate(returnDate);
            updatedLoan.setPenaltyAmount(partialPayment);
            return updatedLoan;
        });

        // Act
        LoanResponseDto response = loanService.returnBook(1L);

        // Assert
        assertNotNull(response);
        assertEquals(LoanStatus.RETURNED, response.getStatus());
        assertEquals(returnDate, response.getReturnDate());
        assertTrue(response.getPenaltyAmount() > 0);
    }

    @Test
    void getLoanHistoryByUser_WithMultipleLoans() {
        // Arrange
        List<Loan> loans = Arrays.asList(
            testLoan,
            Loan.builder()
                .id(2L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(LocalDate.now().minusDays(30))
                .dueDate(LocalDate.now().minusDays(16))
                .returnDate(LocalDate.now().minusDays(15))
                .status(LoanStatus.RETURNED)
                .build(),
            Loan.builder()
                .id(3L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().plusDays(4))
                .status(LoanStatus.BORROWED)
                .build()
        );

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(loanRepository.findByUser_Id(1L)).thenReturn(loans);

        // Act
        List<LoanResponseDto> response = loanService.getLoanHistoryByUser(1L);

        // Assert
        assertNotNull(response);
        assertEquals(3, response.size());
        assertEquals(LoanStatus.BORROWED, response.get(0).getStatus());
        assertEquals(LoanStatus.RETURNED, response.get(1).getStatus());
        assertEquals(LoanStatus.BORROWED, response.get(2).getStatus());
    }

    @Test
    void borrowBook_WithConcurrentBorrows() {
        // Arrange
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED))
            .thenReturn(2L)  // İlk kontrol
            .thenReturn(3L); // İkinci kontrol
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // Act & Assert
        assertThrows(LoanLimitExceededException.class, () -> {
            loanService.borrowBook(loanRequestDto);
            loanService.borrowBook(loanRequestDto);
        });
    }

    @Test
    void generateLoanReport_WithAllLoanTypes() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate pastDate = today.minusDays(20);
        LocalDate futureDate = today.plusDays(5);
        
        List<Loan> loans = Arrays.asList(
            // Overdue loan
            Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDate)
                .dueDate(pastDate.plusDays(14))
                .status(LoanStatus.OVERDUE)
                .penaltyAmount(30.0)
                .build(),
            // Upcoming due loan
            Loan.builder()
                .id(2L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(today)
                .dueDate(futureDate)
                .status(LoanStatus.BORROWED)
                .build(),
            // Returned loan
            Loan.builder()
                .id(3L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDate)
                .dueDate(pastDate.plusDays(14))
                .returnDate(pastDate.plusDays(15))
                .status(LoanStatus.RETURNED)
                .build(),
            // Active loan
            Loan.builder()
                .id(4L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(today.minusDays(5))
                .dueDate(today.plusDays(9))
                .status(LoanStatus.BORROWED)
                .build()
        );

        when(loanRepository.findAll()).thenReturn(loans);
        when(loanRepository.findByStatus(LoanStatus.OVERDUE)).thenReturn(List.of(loans.get(0)));
        when(loanRepository.findByStatus(LoanStatus.BORROWED)).thenReturn(Arrays.asList(loans.get(1), loans.get(3)));
        when(loanRepository.findByStatus(LoanStatus.RETURNED)).thenReturn(List.of(loans.get(2)));

        // Act
        String report = loanService.generateLoanReport();

        // Assert
        assertNotNull(report);
        assertTrue(report.contains("Toplam Ödünç Kitap Sayısı: 4"));
        assertTrue(report.contains("Aktif Ödünç Kitap Sayısı: 2"));
        assertTrue(report.contains("Geciken Kitap Sayısı: 1"));
        assertTrue(report.contains("İade Edilen Kitap Sayısı: 1"));
        assertTrue(report.contains("AKTİF ÖDÜNÇ KİTAPLAR"));
        assertTrue(report.contains("GECİKEN KİTAPLAR"));
        assertTrue(report.contains("SON İADE EDİLEN KİTAPLAR"));
    }

    @Test
    void checkAndUpdateOverdueLoans_WithEmailNotification() {
        // Arrange
        LocalDate pastDueDate = LocalDate.now().minusDays(5);
        Loan overdueLoan = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDueDate.minusDays(14))
                .dueDate(pastDueDate)
                .status(LoanStatus.BORROWED)
                .build();

        when(loanRepository.findByStatusAndDueDateBefore(eq(LoanStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(List.of(overdueLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(overdueLoan);

        // Act
        loanService.checkAndUpdateOverdueLoans();

        // Assert
        verify(emailService).sendOverdueNotification(
            eq(testUser.getEmail()),
            eq(testUser.getUsername()),
            anyList()
        );
    }

    @Test
    void borrowBook_WithEmailNotification() {
        // Arrange
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // Act
        loanService.borrowBook(loanRequestDto);

        // Assert
        verify(emailService).sendLoanNotification(
            eq(testUser.getEmail()),
            eq(testUser.getUsername()),
            eq(testBook.getTitle()),
            eq(fixedDate),
            eq(fixedDate.plusDays(14))
        );
    }

    @Test
    void borrowBook_WithInvalidBookId() {
        // Arrange
        loanRequestDto.setBookId(null);
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(null)).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> loanService.borrowBook(loanRequestDto));
    }

    @Test
    void borrowBook_WithInvalidUserId() {
        // Arrange
        loanRequestDto.setUserId(null);
        when(userService.getUserEntity(null)).thenReturn(null);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> loanService.borrowBook(loanRequestDto));
    }

    @Test
    void returnBook_WithInvalidLoanId() {
        // Act & Assert
        assertThrows(LoanNotFoundException.class, () -> loanService.returnBook(null));
    }

    @Test
    void getLoanById_WithInvalidId() {
        // Act & Assert
        assertThrows(LoanNotFoundException.class, () -> loanService.getLoanById(null));
    }

    @Test
    void getLoanHistoryByUser_WithInvalidUserId() {
        // Act & Assert
        assertThrows(UserLoanHistoryNotFoundException.class, () -> loanService.getLoanHistoryByUser(null));
    }

    @Test
    void borrowBook_WithZeroAvailableCount() {
        // Arrange
        testBook.setAvailableCount(0);
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);

        // Act & Assert
        assertThrows(BookNotAvailableException.class, () -> loanService.borrowBook(loanRequestDto));
    }

    @Test
    void borrowBook_WithNegativeAvailableCount() {
        // Arrange
        testBook.setAvailableCount(-1);
        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);

        // Act & Assert
        assertThrows(BookNotAvailableException.class, () -> loanService.borrowBook(loanRequestDto));
    }

    @Test
    void returnBook_WithMaxPenaltyAmount() {
        // Arrange
        LocalDate borrowedDate = LocalDate.now().minusDays(100);
        LocalDate dueDate = borrowedDate.plusDays(14);
        testLoan.setBorrowedDate(borrowedDate);
        testLoan.setDueDate(dueDate);
        testLoan.setStatus(LoanStatus.OVERDUE);
        testLoan.setPenaltyAmount(500.0); // Maximum penalty amount

        when(loanRepository.findById(1L)).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        // Act
        LoanResponseDto response = loanService.returnBook(1L);

        // Assert
        assertNotNull(response);
        assertEquals(LoanStatus.RETURNED, response.getStatus());
        assertEquals(500.0, response.getPenaltyAmount());
    }

    @Test
    void checkAndUpdateOverdueLoans_WithMultipleUsers() {
        // Arrange
        User user2 = User.builder()
                .id(2L)
                .username("testuser2")
                .email("test2@example.com")
                .build();

        LocalDate pastDueDate = LocalDate.now().minusDays(5);
        Loan overdueLoan1 = Loan.builder()
                .id(1L)
                .book(testBook)
                .user(testUser)
                .borrowedDate(pastDueDate.minusDays(14))
                .dueDate(pastDueDate)
                .status(LoanStatus.BORROWED)
                .build();

        Loan overdueLoan2 = Loan.builder()
                .id(2L)
                .book(testBook)
                .user(user2)
                .borrowedDate(pastDueDate.minusDays(14))
                .dueDate(pastDueDate)
                .status(LoanStatus.BORROWED)
                .build();

        when(loanRepository.findByStatusAndDueDateBefore(eq(LoanStatus.BORROWED), any(LocalDate.class)))
                .thenReturn(Arrays.asList(overdueLoan1, overdueLoan2));
        when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        loanService.checkAndUpdateOverdueLoans();

        // Assert
        verify(emailService, times(2)).sendOverdueNotification(
            anyString(),
            anyString(),
            anyList()
        );
    }

    @Test
    void borrowBook_WithCustomBorrowDateAndDueDate() {
        // Arrange
        LocalDate customBorrowDate = LocalDate.now().minusDays(5);
        LocalDate customDueDate = customBorrowDate.plusDays(21); // 3 hafta
        
        loanRequestDto.setBorrowedDate(customBorrowDate);

        when(userService.getUserEntity(1L)).thenReturn(testUser);
        when(bookService.getBookEntity(1L)).thenReturn(testBook);
        when(loanRepository.countByUser_IdAndStatus(1L, LoanStatus.BORROWED)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setBorrowedDate(customBorrowDate);
            savedLoan.setDueDate(customDueDate);
            return savedLoan;
        });

        // Act
        LoanResponseDto response = loanService.borrowBook(loanRequestDto);

        // Assert
        assertNotNull(response);
        assertEquals(customBorrowDate, response.getBorrowedDate());
        assertEquals(customDueDate, response.getDueDate());
    }
}
