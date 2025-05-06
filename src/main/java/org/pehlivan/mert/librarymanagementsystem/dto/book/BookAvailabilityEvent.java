package org.pehlivan.mert.librarymanagementsystem.dto.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAvailabilityEvent {
    private Long bookId;
    private String bookTitle;
    private boolean available;
    private LocalDateTime timestamp;
    private String eventType;
} 