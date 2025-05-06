package org.pehlivan.mert.librarymanagementsystem.dto.book;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponseDto {

    private Long id;
    private String title;
    private String isbn;
    private Integer stock;
    private Integer availableCount;
    private Integer pageCount;
    private Date publicationDate;
    private String publisher;
    private BookStatus status;
    private BookType bookType;
    private Long authorId;
    private String authorName;
    private String authorSurname;
}
