package org.pehlivan.mert.librarymanagementsystem.service.loan;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.OverdueLoanReportResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotAvailableException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanAlreadyReturnedException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanLimitExceededException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanNotFoundException;
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
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
@CacheConfig(cacheNames = "loans", cacheManager = "redisCacheManager")
public class LoanService {
   
    private final LoanRepository loanRepository;
    private final BookService bookService;
    private final UserService userService;
    private final EmailService emailService;
    private final MeterRegistry meterRegistry;

    private Counter loanCounter;
    private Counter overdueCounter;

    @PostConstruct
    public void init() {
        loanCounter = Counter.builder("library.loans.total")
                .description("Total number of book loans")
                .register(meterRegistry);

        overdueCounter = Counter.builder("library.loans.overdue")
                .description("Total number of overdue loans")
                .register(meterRegistry);
    }

    private static final int MAX_LOANS_PER_USER = 3;
    private static final int LOAN_PERIOD_DAYS = 14;

    @Scheduled(fixedRate = 90000) 
    @CacheEvict(allEntries = true)
    public void checkAndUpdateOverdueLoans() {
        log.info("Checking for overdue loans");
        LocalDate today = LocalDate.now();
        
        List<Loan> overdueLoans = loanRepository.findByStatusAndDueDateBefore(LoanStatus.BORROWED, today);
        
        if (!overdueLoans.isEmpty()) {
            log.info("Found {} overdue loans", overdueLoans.size());
            
            for (Loan loan : overdueLoans) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
                log.info("Updated loan {} to OVERDUE status", loan.getId());
                
                overdueCounter.increment();
                
                Map<String, Object> bookInfo = new HashMap<>();
                bookInfo.put("title", loan.getBook().getTitle());
                bookInfo.put("borrowedDate", loan.getBorrowedDate());
                bookInfo.put("dueDate", loan.getDueDate());
                bookInfo.put("overdueDays", ChronoUnit.DAYS.between(loan.getDueDate(), today));
                
                emailService.sendOverdueNotification(
                    loan.getUser().getEmail(),
                    loan.getUser().getUsername(),
                    List.of(bookInfo)
                );
            }
        } else {
            log.info("No overdue loans found");
        }
    }

    @CacheEvict(key = "{'loan:user:' + #loanRequestDto.userId, 'loan:book:' + #loanRequestDto.bookId}")
    public LoanResponseDto borrowBook(LoanRequestDto loanRequestDto) {
        log.info("Borrowing book with request: {}", loanRequestDto);
        
        User user = userService.getUserEntity(loanRequestDto.getUserId());
        Book book = bookService.getBookEntity(loanRequestDto.getBookId());
        
        if (book.getAvailableCount() <= 0) {
            throw new BookNotAvailableException("Book is not available for loan");
        }
        
        validateBorrowRequest(loanRequestDto);
        
        LocalDate borrowedDate = loanRequestDto.getBorrowedDate() != null ? loanRequestDto.getBorrowedDate() : LocalDate.now();
        LocalDate dueDate = borrowedDate.plusDays(LOAN_PERIOD_DAYS);
        
        Loan loan = Loan.builder()
                .book(book)
                .user(user)
                .borrowedDate(borrowedDate)
                .dueDate(dueDate)
                .status(LoanStatus.BORROWED)
                .build();

        Loan savedLoan = loanRepository.save(loan);
        
        loanCounter.increment();
        
        try {
            bookService.decreaseAvailableCount(book.getId());
        } catch (BookNotAvailableException e) {
            loanRepository.delete(savedLoan);
            throw e;
        }
        
        emailService.sendLoanNotification(
            user.getEmail(),
            user.getUsername(),
            book.getTitle(),
            borrowedDate,
            dueDate
        );
        
        return convertToDto(savedLoan);
    }

    @CacheEvict(key = "{'loan:' + #loanId, 'loan:user:' + #result.userId, 'loan:book:' + #result.bookId}")
    public LoanResponseDto returnBook(Long loanId) {
        log.info("Returning book for loan: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found with id: " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new LoanAlreadyReturnedException("Loan with id " + loanId + " is already returned");
        }

        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);

        Loan updatedLoan = loanRepository.save(loan);
        
        bookService.increaseAvailableCount(loan.getBook().getId());
        
        return convertToDto(updatedLoan);
    }

    @Cacheable(key = "'loan:' + #loanId", unless = "#result == null")
    public LoanResponseDto getLoanById(Long loanId) {
        log.info("Getting loan by id: {}", loanId);
        return loanRepository.findById(loanId)
                .map(this::convertToDto)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found with id: " + loanId));
    }

    @Cacheable(key = "'all-loans'", unless = "#result.isEmpty()")
    public List<LoanResponseDto> getAllLoanHistory() {
        log.info("Getting all loan history");
        List<Loan> loans = loanRepository.findAll();
        if (loans.isEmpty()) {
            throw new LoanNotFoundException("No loan history found");
        }
        return loans.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(key = "'loan:user:' + #userId", unless = "#result.isEmpty()")
    public List<LoanResponseDto> getLoanHistoryByUser(Long userId) {
        log.info("Getting loan history for user: {}", userId);
        
        try {
            userService.getUser(userId);
        } catch (Exception e) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        
        List<Loan> loans = loanRepository.findByUser_Id(userId);
        if (loans.isEmpty()) {
            throw new UserLoanHistoryNotFoundException("No loan history found for user with id: " + userId);
        }
        
        return loans.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(key = "'late-loans'", unless = "#result.isEmpty()")
    public List<LoanResponseDto> getLateLoans() {
        log.info("Getting all late loans");
        return loanRepository.findByStatus(LoanStatus.OVERDUE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(key = "'overdue-report'", unless = "#result == null")
    public OverdueLoanReportResponseDto getOverdueLoanReport() {
        log.info("Generating overdue loan report");
        List<Loan> overdueLoans = loanRepository.findByStatus(LoanStatus.OVERDUE);
        List<OverdueLoanReportResponseDto.OverdueLoanItem> reportItems = overdueLoans.stream().map(loan -> {
            long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            return OverdueLoanReportResponseDto.OverdueLoanItem.builder()
                    .loanId(loan.getId())
                    .bookTitle(loan.getBook().getTitle())
                    .bookIsbn(loan.getBook().getIsbn())
                    .borrowerName(loan.getUser().getUsername())
                    .dueDate(loan.getDueDate())
                    .returnDate(loan.getReturnDate())
                    .daysOverdue(daysOverdue)
                    .status(loan.getStatus().name())
                    .build();
        }).collect(Collectors.toList());
        
        return OverdueLoanReportResponseDto.create(reportItems);
    }

    private void validateBorrowRequest(LoanRequestDto loanRequestDto) {
        long activeLoans = loanRepository.countByUser_IdAndStatus(loanRequestDto.getUserId(), LoanStatus.BORROWED);
        if (activeLoans >= MAX_LOANS_PER_USER) {
            throw new LoanLimitExceededException("User has reached maximum loan limit of " + MAX_LOANS_PER_USER);
        }
    }

    private LoanResponseDto convertToDto(Loan loan) {
        return LoanResponseDto.builder()
                .id(loan.getId())
                .bookId(loan.getBook().getId())
                .bookTitle(loan.getBook().getTitle())
                .userId(loan.getUser().getId())
                .userName(loan.getUser().getUsername())
                .borrowedDate(loan.getBorrowedDate())
                .dueDate(loan.getDueDate())
                .returnDate(loan.getReturnDate())
                .status(loan.getStatus())
                .build();
    }
}

