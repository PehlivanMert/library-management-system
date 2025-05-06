package org.pehlivan.mert.librarymanagementsystem.repository.loan;

import org.pehlivan.mert.librarymanagementsystem.model.loan.Loan;
import org.pehlivan.mert.librarymanagementsystem.model.loan.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);
    List<Loan> findByStatus(LoanStatus status);
    List<Loan> findByUser_Id(Long userId);
    long countByUser_IdAndStatus(Long userId, LoanStatus status);
}
