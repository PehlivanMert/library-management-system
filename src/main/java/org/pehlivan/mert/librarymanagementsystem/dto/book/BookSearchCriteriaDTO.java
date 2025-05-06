package org.pehlivan.mert.librarymanagementsystem.dto.book;

import lombok.Data;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;

@Data
public class BookSearchCriteriaDTO {
    private String title;
    private String authorName;
    private String authorSurname;
    private String isbn;
    private BookType bookType;
    private BookStatus status;
} 