package org.pehlivan.mert.librarymanagementsystem.service.loan;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.pehlivan.mert.librarymanagementsystem.model.loan.Loan;
import org.pehlivan.mert.librarymanagementsystem.repository.loan.LoanRepository;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanLimitExceededException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotAvailableException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.LoanAlreadyReturnedException;
import org.pehlivan.mert.librarymanagementsystem.exception.loan.UserLoanHistoryNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.exception.user.UserNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.model.loan.LoanStatus;
import org.pehlivan.mert.librarymanagementsystem.service.book.BookService;
import org.pehlivan.mert.librarymanagementsystem.service.user.UserService;
import org.pehlivan.mert.librarymanagementsystem.service.email.EmailService;
import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.UserLoanRequestDto;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
@CacheConfig(cacheNames = "loan", cacheManager = "redisCacheManager")
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
        loanCounter    = meterRegistry.counter("library.loans.total");
        overdueCounter = meterRegistry.counter("library.loans.overdue");
    }

    private static final int MAX_LOANS_PER_USER = 3;
    private static final int LOAN_PERIOD_DAYS = 14;
    private static final double DAILY_PENALTY_AMOUNT = 5.0;
    private static final int MAX_EMAIL_DAYS = 30;

    //@Scheduled(cron = "0 0 8 * * *") // Her gün saat 8'da
    @Scheduled(fixedRate = 30000) // Test Her 30 saniyede bir
    @CacheEvict(allEntries = true)
    public void checkAndUpdateOverdueLoans() {
        log.info("Checking for overdue loans");
        LocalDate today = LocalDate.now();

        List<Loan> overdueLoans = loanRepository.findByStatusAndDueDateBefore(LoanStatus.BORROWED, today);

        if (!overdueLoans.isEmpty()) {
            log.info("Found {} overdue loans", overdueLoans.size());

            for (Loan loan : overdueLoans) {
                long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), today);

                // Her durumda ceza hesapla
                double penaltyAmount = daysOverdue * DAILY_PENALTY_AMOUNT;
                loan.setPenaltyAmount(penaltyAmount);
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
                log.info("Updated loan {} to OVERDUE status with penalty amount: {}", loan.getId(), penaltyAmount);

                overdueCounter.increment();

                // Sadece 30 güne kadar mail gönder
                if (daysOverdue <= MAX_EMAIL_DAYS) {
                    Map<String, Object> bookInfo = new HashMap<>();
                    bookInfo.put("title", loan.getBook().getTitle());
                    bookInfo.put("borrowedDate", loan.getBorrowedDate());
                    bookInfo.put("dueDate", loan.getDueDate());
                    bookInfo.put("overdueDays", daysOverdue);
                    bookInfo.put("penaltyAmount", penaltyAmount);

                    emailService.sendOverdueNotification(
                            loan.getUser().getEmail(),
                            loan.getUser().getUsername(),
                            List.of(bookInfo)
                    );
                }
            }
        } else {
            log.info("No overdue loans found");
        }
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public LoanResponseDto borrowBook(LoanRequestDto loanRequestDto) {
        log.info("Borrowing book with request: {}", loanRequestDto);

        User user = userService.getUserEntity(loanRequestDto.getUserId());
        Book book = bookService.getBookEntity(loanRequestDto.getBookId());

        if (book.getAvailableCount() <= 0) {
            throw new BookNotAvailableException("Book is not available for loan");
        }

        validateBorrowRequest(loanRequestDto);

        LocalDate borrowedDate = loanRequestDto.getBorrowedDate() != null ?
                loanRequestDto.getBorrowedDate() : LocalDate.now();
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

    @Transactional
    @CacheEvict(allEntries = true)
    public LoanResponseDto borrowBookForUser(UserLoanRequestDto userLoanRequestDto, Long userId) {
        log.info("User {} borrowing book with request: {}", userId, userLoanRequestDto);

        User user = userService.getUserEntity(userId);
        Book book = bookService.getBookEntity(userLoanRequestDto.getBookId());

        if (book.getAvailableCount() <= 0) {
            throw new BookNotAvailableException("Book is not available for loan");
        }

        validateBorrowRequest(LoanRequestDto.builder()
                .userId(userId)
                .bookId(userLoanRequestDto.getBookId())
                .borrowedDate(userLoanRequestDto.getBorrowedDate())
                .build());

        LocalDate borrowedDate = userLoanRequestDto.getBorrowedDate() != null ?
                userLoanRequestDto.getBorrowedDate() : LocalDate.now();
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

    @Transactional
    @CacheEvict(allEntries = true)
    public LoanResponseDto returnBook(Long loanId) {
        log.info("Returning book for loan: {}", loanId);

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found with id: " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new LoanAlreadyReturnedException("Loan with id " + loanId + " is already returned");
        }

        LocalDate returnDate = LocalDate.now();
        loan.setReturnDate(returnDate);
        loan.setStatus(LoanStatus.RETURNED);

        // Eğer kitap gecikmiş ise, iade tarihine kadar olan cezayı hesapla
        if (loan.getStatus() == LoanStatus.OVERDUE) {
            long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
            double finalPenalty = daysOverdue * DAILY_PENALTY_AMOUNT;
            loan.setPenaltyAmount(finalPenalty);
            log.info("Final penalty amount calculated for loan {}: {} TL", loanId, finalPenalty);
        }

        Loan updatedLoan = loanRepository.save(loan);

        bookService.increaseAvailableCount(loan.getBook().getId());

        return convertToDto(updatedLoan);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'id:' + #loanId", unless = "#result == null")
    public LoanResponseDto getLoanById(Long loanId) {
        log.info("Getting loan by id: {}", loanId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found with id: " + loanId));

        // Check if current user is a reader and trying to access someone else's loan
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_READER"))) {
            String currentEmail = authentication.getName();
            if (!loan.getUser().getEmail().equals(currentEmail)) {
                log.error("Access denied: Reader {} trying to access loan {} of another user",
                        currentEmail, loanId);
                throw new AccessDeniedException("Readers can only access their own loans");
            }
        }

        return convertToDto(loan);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'all'", unless = "#result.isEmpty()")
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

    @Transactional(readOnly = true)
    @Cacheable(key = "'user:' + #userId", unless = "#result.isEmpty()")
    public List<LoanResponseDto> getLoanHistoryByUser(Long userId) {
        log.info("Getting loan history for user: {}", userId);

        // Check if current user is a reader and trying to access someone else's history
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_READER"))) {
            String currentEmail = authentication.getName();
            User user = userService.getUserEntity(userId);
            if (!user.getEmail().equals(currentEmail)) {
                log.error("Access denied: Reader {} trying to access loan history of user {}",
                        currentEmail, userId);
                throw new AccessDeniedException("Readers can only access their own loan history");
            }
        }

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

    @Transactional(readOnly = true)
    @Cacheable(key = "'late'", unless = "#result.isEmpty()")
    public List<LoanResponseDto> getLateLoans() {
        log.info("Getting all late loans");
        return loanRepository.findByStatus(LoanStatus.OVERDUE).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'report'", unless = "#result == null")
    public String generateLoanReport() {
        log.info("Generating loan report");

        List<Loan> allLoans = loanRepository.findAll();
        if (allLoans.isEmpty()) {
            throw new LoanNotFoundException("No loans found to generate report");
        }

        LocalDate today = LocalDate.now();

        // Kategorilere göre ayır
        List<Loan> overdueLoans = allLoans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.OVERDUE)
                .sorted(Comparator.comparing(Loan::getDueDate))
                .collect(Collectors.toList());

        List<Loan> upcomingDueLoans = allLoans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.BORROWED &&
                        loan.getDueDate().isAfter(today) &&
                        loan.getDueDate().isBefore(today.plusDays(7)))
                .sorted(Comparator.comparing(Loan::getDueDate))
                .collect(Collectors.toList());

        List<Loan> returnedLoans = allLoans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.RETURNED)
                .sorted(Comparator.comparing(Loan::getReturnDate).reversed())
                .collect(Collectors.toList());

        List<Loan> activeLoans = allLoans.stream()
                .filter(loan -> loan.getStatus() == LoanStatus.BORROWED)
                .sorted(Comparator.comparing(Loan::getDueDate))
                .collect(Collectors.toList());

        // Rapor oluştur
        StringBuilder report = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        report.append("KÜTÜPHANE ÖDÜNÇ KİTAP RAPORU\n");
        report.append("Oluşturulma Tarihi: ").append(today.format(formatter)).append("\n\n");

        report.append("GENEL İSTATİSTİKLER\n");
        report.append("===================\n");
        report.append("Toplam Ödünç Kitap Sayısı: ").append(allLoans.size()).append("\n");
        report.append("Aktif Ödünç Kitap Sayısı: ").append(activeLoans.size()).append("\n");
        report.append("Geciken Kitap Sayısı: ").append(overdueLoans.size()).append("\n");
        report.append("Yaklaşan Son Tarihli Kitap Sayısı: ").append(upcomingDueLoans.size()).append("\n");
        report.append("İade Edilen Kitap Sayısı: ").append(returnedLoans.size()).append("\n\n");

        report.append("AKTİF ÖDÜNÇ KİTAPLAR\n");
        report.append("===================\n");
        if (activeLoans.isEmpty()) {
            report.append("Aktif ödünç kitap bulunmamaktadır.\n");
        } else {
            report.append(String.format("%-5s %-30s %-20s %-15s %-15s %-10s\n",
                    "ID", "Kitap Adı", "Ödünç Alan", "Ödünç Tarihi", "Son Tarih", "Kalan Gün"));
            report.append("-".repeat(100)).append("\n");

            for (Loan loan : activeLoans) {
                long daysRemaining = ChronoUnit.DAYS.between(today, loan.getDueDate());
                report.append(String.format("%-5d %-30s %-20s %-15s %-15s %-10d\n",
                        loan.getId(),
                        loan.getBook().getTitle(),
                        loan.getUser().getUsername(),
                        loan.getBorrowedDate().format(formatter),
                        loan.getDueDate().format(formatter),
                        daysRemaining));
            }
        }

        report.append("\nGECİKEN KİTAPLAR\n");
        report.append("================\n");
        if (overdueLoans.isEmpty()) {
            report.append("Geciken kitap bulunmamaktadır.\n");
        } else {
            report.append(String.format("%-5s %-30s %-20s %-15s %-15s %-10s %-10s\n",
                    "ID", "Kitap Adı", "Ödünç Alan", "Ödünç Tarihi", "Son Tarih", "Gecikme (Gün)", "Ceza (TL)"));
            report.append("-".repeat(110)).append("\n");

            for (Loan loan : overdueLoans) {
                long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), today);
                report.append(String.format("%-5d %-30s %-20s %-15s %-15s %-10d %-10.2f\n",
                        loan.getId(),
                        loan.getBook().getTitle(),
                        loan.getUser().getUsername(),
                        loan.getBorrowedDate().format(formatter),
                        loan.getDueDate().format(formatter),
                        daysOverdue,
                        loan.getPenaltyAmount()));
            }
        }

        report.append("\nYAKLAŞAN SON TARİHLİ KİTAPLAR\n");
        report.append("===========================\n");
        if (upcomingDueLoans.isEmpty()) {
            report.append("Yaklaşan son tarihli kitap bulunmamaktadır.\n");
        } else {
            report.append(String.format("%-5s %-30s %-20s %-15s %-15s\n",
                    "ID", "Kitap Adı", "Ödünç Alan", "Ödünç Tarihi", "Son Tarih"));
            report.append("-".repeat(90)).append("\n");

            for (Loan loan : upcomingDueLoans) {
                report.append(String.format("%-5d %-30s %-20s %-15s %-15s\n",
                        loan.getId(),
                        loan.getBook().getTitle(),
                        loan.getUser().getUsername(),
                        loan.getBorrowedDate().format(formatter),
                        loan.getDueDate().format(formatter)));
            }
        }

        report.append("\nSON İADE EDİLEN KİTAPLAR\n");
        report.append("=======================\n");
        if (returnedLoans.isEmpty()) {
            report.append("İade edilen kitap bulunmamaktadır.\n");
        } else {
            report.append(String.format("%-5s %-30s %-20s %-15s %-15s\n",
                    "ID", "Kitap Adı", "Ödünç Alan", "İade Tarihi", "Durum"));
            report.append("-".repeat(90)).append("\n");

            for (Loan loan : returnedLoans) {
                report.append(String.format("%-5d %-30s %-20s %-15s %-15s\n",
                        loan.getId(),
                        loan.getBook().getTitle(),
                        loan.getUser().getUsername(),
                        loan.getReturnDate().format(formatter),
                        loan.getStatus()));
            }
        }

        // Dosyaya kaydet
        String fileName = "loan_report_" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".txt";
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(report.toString());
            log.info("Loan report saved to file: {}", fileName);
        } catch (IOException e) {
            log.error("Error saving loan report to file: {}", e.getMessage());
            throw new RuntimeException("Error saving loan report to file", e);
        }

        return report.toString();
    }

    private void validateBorrowRequest(LoanRequestDto loanRequestDto) {
        long activeLoans = loanRepository.countByUser_IdAndStatus(loanRequestDto.getUserId(), LoanStatus.BORROWED);
        if (activeLoans >= MAX_LOANS_PER_USER) {
            throw new LoanLimitExceededException("User has reached maximum loan limit of " + MAX_LOANS_PER_USER);
        }

        // Geciken kitabı olan kullanıcıların yeni kitap almasını engelle
        List<Loan> overdueLoans = loanRepository.findByUser_IdAndStatus(loanRequestDto.getUserId(), LoanStatus.OVERDUE);
        if (!overdueLoans.isEmpty()) {
            throw new LoanLimitExceededException("User has overdue books that need to be returned before borrowing new books");
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
                .penaltyAmount(loan.getPenaltyAmount())
                .build();
    }
}

