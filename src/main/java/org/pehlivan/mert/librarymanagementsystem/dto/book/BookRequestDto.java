package org.pehlivan.mert.librarymanagementsystem.dto.book;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.ISBN;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequestDto {

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @NotBlank(message = "ISBN is required")
    @ISBN(message = "ISBN is invalid")
    @Size(min = 10, max = 13, message = "ISBN must be between 10 and 13 characters")
    private String isbn;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Max(value = 1000, message = "Stock cannot exceed 1000")
    private Integer stock;

    @NotNull(message = "Page Count is required")
    @Positive(message = "Page Count must be positive")
    @Max(value = 10000, message = "Page Count cannot exceed 10000")
    private Integer pageCount;

    @NotNull(message = "Publication Date is required")
    @PastOrPresent(message = "Publication Date cannot be in the future")
    private Date publicationDate;

    @NotBlank(message = "Publisher is required")
    @Size(min = 2, max = 100, message = "Publisher must be between 2 and 100 characters")
    private String publisher;

    @NotNull(message = "Book Type is required")
    private BookType bookType;

    @NotBlank(message = "Author Name is required")
    private String authorName;

    @NotBlank(message = "Author Surname is required")
    private String authorSurname;
}
