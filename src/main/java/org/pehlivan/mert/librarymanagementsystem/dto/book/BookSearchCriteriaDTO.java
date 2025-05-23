package org.pehlivan.mert.librarymanagementsystem.dto.book;

import lombok.Data;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;

@Data
public class BookSearchCriteriaDTO {
    private String title;
    private String authorName;
    private String authorSurname;
    private String isbn;
    private BookType bookType;
    private BookStatus status;
} 