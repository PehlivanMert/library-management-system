package org.pehlivan.mert.librarymanagementsystem.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;
import org.pehlivan.mert.librarymanagementsystem.service.author.AuthorService;
import org.pehlivan.mert.librarymanagementsystem.service.book.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BookServiceIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private AuthorService authorService;

    private BookRequestDto bookRequestDto;
    private Long authorId;

    @BeforeEach
    void setUp() {
        // Create author first
        AuthorRequestDto authorRequestDto = AuthorRequestDto.builder()
                .name("Test")
                .surname("Author")
                .build();
        authorId = authorService.createAuthor(authorRequestDto).getId();

        // Create book request
        bookRequestDto = BookRequestDto.builder()
                .title("Test Book")
                .isbn("9780306406157")
                .stock(5)
                .pageCount(200)
                .publicationDate(new Date())
                .publisher("Test Publisher")
                .authorName("Test")
                .authorSurname("Author")
                .bookType(BookType.FICTION)
                .build();
    }

    @Test
    void createBook_Success() {
        // When
        BookResponseDto createdBook = bookService.createBook(bookRequestDto);

        // Then
        assertNotNull(createdBook);
        assertNotNull(createdBook.getId());
        assertEquals(bookRequestDto.getTitle(), createdBook.getTitle());
        assertEquals(bookRequestDto.getIsbn(), createdBook.getIsbn());
        assertEquals(bookRequestDto.getStock(), createdBook.getStock());
        assertEquals(bookRequestDto.getStock(), createdBook.getAvailableCount());
        assertEquals(bookRequestDto.getPageCount(), createdBook.getPageCount());
        assertEquals(bookRequestDto.getPublicationDate(), createdBook.getPublicationDate());
        assertEquals(bookRequestDto.getPublisher(), createdBook.getPublisher());
        assertEquals(bookRequestDto.getAuthorName(), createdBook.getAuthorName());
        assertEquals(bookRequestDto.getAuthorSurname(), createdBook.getAuthorSurname());
        assertEquals(bookRequestDto.getBookType(), createdBook.getBookType());
        assertEquals(BookStatus.AVAILABLE, createdBook.getStatus());
    }

    @Test
    void getBookById_Success() {
        // Given
        BookResponseDto createdBook = bookService.createBook(bookRequestDto);

        // When
        BookResponseDto foundBook = bookService.getBookById(createdBook.getId());

        // Then
        assertNotNull(foundBook);
        assertEquals(createdBook.getId(), foundBook.getId());
        assertEquals(createdBook.getTitle(), foundBook.getTitle());
        assertEquals(createdBook.getIsbn(), foundBook.getIsbn());
        assertEquals(createdBook.getStock(), foundBook.getStock());
        assertEquals(createdBook.getAvailableCount(), foundBook.getAvailableCount());
        assertEquals(createdBook.getStatus(), foundBook.getStatus());
    }

    @Test
    void getBookById_NotFound() {
        // When & Then
        assertThrows(BookNotFoundException.class, () -> bookService.getBookById(999L));
    }

    @Test
    void updateBook_Success() {
        // Given
        BookResponseDto createdBook = bookService.createBook(bookRequestDto);
        BookRequestDto updateRequest = BookRequestDto.builder()
                .title("Updated Book")
                .isbn(createdBook.getIsbn())
                .stock(10)
                .pageCount(300)
                .publicationDate(new Date())
                .publisher("Updated Publisher")
                .authorName("Test")
                .authorSurname("Author")
                .bookType(BookType.NON_FICTION)
                .build();

        // When
        BookResponseDto updatedBook = bookService.updateBook(createdBook.getId(), updateRequest);

        // Then
        assertNotNull(updatedBook);
        assertEquals(createdBook.getId(), updatedBook.getId());
        assertEquals(updateRequest.getTitle(), updatedBook.getTitle());
        assertEquals(updateRequest.getStock(), updatedBook.getStock());
        assertEquals(updateRequest.getPageCount(), updatedBook.getPageCount());
        assertEquals(updateRequest.getPublisher(), updatedBook.getPublisher());
        assertEquals(updateRequest.getBookType(), updatedBook.getBookType());
    }

    @Test
    void deleteBook_Success() {
        // Given
        BookResponseDto createdBook = bookService.createBook(bookRequestDto);

        // When
        bookService.deleteBook(createdBook.getId());

        // Then
        assertThrows(BookNotFoundException.class, () -> bookService.getBookById(createdBook.getId()));
    }

    @Test
    void getAllBooks_Success() {
        // Given
        bookService.createBook(bookRequestDto);
        BookRequestDto secondBook = BookRequestDto.builder()
                .title("Second Book")
                .isbn("9780306406158")
                .stock(3)
                .pageCount(150)
                .publicationDate(new Date())
                .publisher("Test Publisher")
                .authorName("Test")
                .authorSurname("Author")
                .bookType(BookType.FICTION)
                .build();
        bookService.createBook(secondBook);

        // When
        List<BookResponseDto> books = bookService.getAllBooks();

        // Then
        assertNotNull(books);
        assertTrue(books.size() >= 2);
    }
}