package org.pehlivan.mert.librarymanagementsystem.dto.loan;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueLoanReportResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String title;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime generatedAt;
    
    private int totalOverdueLoans;
    private List<OverdueLoanItem> overdueLoans;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverdueLoanItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long loanId;
        private String bookTitle;
        private String bookIsbn;
        private String borrowerName;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        private LocalDate dueDate;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        private LocalDate returnDate;
        
        private long daysOverdue;
        private String status;
    }

    public static OverdueLoanReportResponseDto create(List<OverdueLoanItem> overdueLoans) {
        return OverdueLoanReportResponseDto.builder()
                .title("Gecikmiş Kitap Ödünç Raporu")
                .description("Bu rapor, kütüphaneden ödünç alınan ve teslim tarihi geçmiş kitapların detaylı listesini içermektedir.")
                .generatedAt(LocalDateTime.now())
                .totalOverdueLoans(overdueLoans.size())
                .overdueLoans(overdueLoans)
                .build();
    }
} 