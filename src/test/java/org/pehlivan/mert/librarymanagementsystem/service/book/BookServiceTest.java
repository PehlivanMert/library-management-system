package org.pehlivan.mert.librarymanagementsystem.service.book;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookSearchCriteriaDTO;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookAlreadyExistsException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotAvailableException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookStockException;
import org.pehlivan.mert.librarymanagementsystem.exception.author.AuthorNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.model.book.Author;
import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;
import org.pehlivan.mert.librarymanagementsystem.repository.author.AuthorRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.book.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookAvailabilityService bookAvailabilityService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter totalBooksCounter;

    @Mock
    private Counter categoryBooksCounter;

    @Mock
    private Counter stockChangeCounter;

    @InjectMocks
    private BookService bookService;

    private BookRequestDto bookRequestDto;
    private Book book;
    private Author author;

    @BeforeEach
    void setUp() throws Exception {
        Date date = new Date(1609459200000L); // 2021-01-01

        author = Author.builder().id(1L).name("Test").surname("Author").build();

        bookRequestDto = BookRequestDto.builder()
                .title("Test Book")
                .isbn("1234567890")
                .stock(10)
                .pageCount(200)
                .publicationDate(date)
                .publisher("Test Publisher")
                .bookType(BookType.FICTION)
                .authorName("Test")
                .authorSurname("Author")
                .build();

        book = Book.builder()
                .id(1L)
                .title("Test Book")
                .isbn("1234567890")
                .stock(10)
                .availableCount(10)
                .pageCount(200)
                .publicationDate(date)
                .publisher("Test Publisher")
                .bookType(BookType.FICTION)
                .status(BookStatus.AVAILABLE)
                .author(author)
                .build();

        // Inject counters
        Field totalField = BookService.class.getDeclaredField("totalBooksCounter");
        Field categoryField = BookService.class.getDeclaredField("categoryBooksCounter");
        Field stockField = BookService.class.getDeclaredField("stockChangeCounter");
        totalField.setAccessible(true);
        categoryField.setAccessible(true);
        stockField.setAccessible(true);
        totalField.set(bookService, totalBooksCounter);
        categoryField.set(bookService, categoryBooksCounter);
        stockField.set(bookService, stockChangeCounter);

        // Default lenient behaviors
        lenient().when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
        lenient().when(authorRepository.findByNameAndSurname(anyString(), anyString()))
                .thenReturn(Optional.of(author));
        lenient().when(bookRepository.existsByTitleAndAuthor_Id(anyString(), anyLong())).thenReturn(false);
        lenient().when(bookRepository.save(any(Book.class))).thenReturn(book);
        lenient().when(bookRepository.findByIdWithAuthor(anyLong())).thenReturn(book);
        lenient().when(bookRepository.findById(anyLong())).thenReturn(Optional.of(book));
        lenient().when(bookRepository.existsById(anyLong())).thenReturn(true);
        lenient().doNothing().when(bookAvailabilityService).notifyAvailabilityChange(any(Book.class), anyString());
    }

    // ----------------------------------------------------------------
    // createBook tests
    // ----------------------------------------------------------------

    @Test
    void createBook_Success() {
        // Given
        when(authorRepository.findByNameAndSurname("Test", "Author")).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        // When
        BookResponseDto result = bookService.createBook(bookRequestDto);

        // Then
        assertEquals("Test Book", result.getTitle());
        verify(bookAvailabilityService).notifyAvailabilityChange(any(Book.class), eq("CREATED"));
        verify(totalBooksCounter).increment();
        verify(categoryBooksCounter).increment();
        verify(stockChangeCounter).increment(10);
    }

    @Test
    void createBook_AuthorNotFound_Throws() {
        // Given
        when(authorRepository.findByNameAndSurname("Test", "Author")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AuthorNotFoundException.class, () -> bookService.createBook(bookRequestDto));
    }

    @Test
    void createBook_DuplicateIsbnOrTitle_Throws() {
        when(bookRepository.existsByIsbn(anyString())).thenReturn(true);
        assertThrows(BookAlreadyExistsException.class, () -> bookService.createBook(bookRequestDto));
        
        when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
        when(bookRepository.existsByTitleAndAuthor_Id(anyString(), anyLong())).thenReturn(true);
        assertThrows(BookAlreadyExistsException.class, () -> bookService.createBook(bookRequestDto));
    }

    // ----------------------------------------------------------------
    // getAllBooks tests
    // ----------------------------------------------------------------

    @Test
    void getAllBooks_Empty_ReturnsEmpty() {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());
        List<BookResponseDto> result = bookService.getAllBooks();
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllBooks_NonEmpty_ReturnsList() {
        when(bookRepository.findAll()).thenReturn(List.of(book));
        List<BookResponseDto> result = bookService.getAllBooks();
        assertEquals(1, result.size());
    }

    // ----------------------------------------------------------------
    // getBookById tests
    // ----------------------------------------------------------------

    @Test
    void getBookById_Success() {
        BookResponseDto dto = bookService.getBookById(1L);
        assertEquals(1L, dto.getId());
    }

    @Test
    void getBookById_NotFound_Throws() {
        when(bookRepository.findByIdWithAuthor(anyLong())).thenReturn(null);
        assertThrows(BookNotFoundException.class, () -> bookService.getBookById(1L));
    }

    @Test
    void getBookById_AuthorNull_Throws() {
        book.setAuthor(null);
        when(bookRepository.findByIdWithAuthor(anyLong())).thenReturn(book);
        assertThrows(IllegalStateException.class, () -> bookService.getBookById(1L));
    }

    // ----------------------------------------------------------------
    // searchBooks tests
    // ----------------------------------------------------------------

    @Test
    void searchBooks_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> page = new PageImpl<>(List.of(book), pageable, 1);
        when(bookRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        Page<BookResponseDto> result = bookService.searchBooks(new BookSearchCriteriaDTO(), pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchBooks_WithEmptyResult_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        BookSearchCriteriaDTO criteria = new BookSearchCriteriaDTO();
        Page<Book> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(bookRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);
        
        Page<BookResponseDto> result = bookService.searchBooks(criteria, pageable);
        
        assertTrue(result.isEmpty());
        verify(bookRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchBooks_WithResults_ReturnsCorrectPage() {
        Pageable pageable = PageRequest.of(0, 10);
        BookSearchCriteriaDTO criteria = new BookSearchCriteriaDTO();
        Page<Book> bookPage = new PageImpl<>(List.of(book), pageable, 1);
        when(bookRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(bookPage);
        
        Page<BookResponseDto> result = bookService.searchBooks(criteria, pageable);
        
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(book.getTitle(), result.getContent().get(0).getTitle());
        verify(bookRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ----------------------------------------------------------------
    // deleteBook tests
    // ----------------------------------------------------------------

    @Test
    void deleteBook_Success() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        assertDoesNotThrow(() -> bookService.deleteBook(1L));
        verify(bookRepository).deleteById(1L);
    }

    @Test
    void deleteBook_NotFound_Throws() {
        when(bookRepository.existsById(1L)).thenReturn(false);
        assertThrows(BookNotFoundException.class, () -> bookService.deleteBook(1L));
    }

    // ----------------------------------------------------------------
    // updateBook tests
    // ----------------------------------------------------------------

    @Test
    void updateBook_IdNull_Throws() {
        assertThrows(IllegalArgumentException.class, () -> bookService.updateBook(null, bookRequestDto));
    }

    @Test
    void updateBook_NotFound_Throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BookNotFoundException.class, () -> bookService.updateBook(1L, bookRequestDto));
    }

    @Test
    void updateBook_DuplicateIsbn_Throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.existsByIsbn(anyString())).thenReturn(true);
        bookRequestDto.setIsbn("otherIsbn");
        assertThrows(BookAlreadyExistsException.class, () -> bookService.updateBook(1L, bookRequestDto));
    }

    @Test
    void updateBook_DuplicateTitleAuthor_Throws() {
        // Given
        Book existingBook = Book.builder()
                .id(1L)
                .title("Existing Book")
                .isbn("1234567890")
                .stock(10)
                .availableCount(10)
                .pageCount(200)
                .publicationDate(new Date())
                .publisher("Test Publisher")
                .bookType(BookType.FICTION)
                .status(BookStatus.AVAILABLE)
                .author(author)
                .build();

        BookRequestDto updateRequest = BookRequestDto.builder()
                .title("New Title")
                .isbn("1234567890")
                .stock(10)
                .pageCount(200)
                .publicationDate(new Date())
                .publisher("Test Publisher")
                .bookType(BookType.FICTION)
                .authorName("Test")
                .authorSurname("Author")
                .build();

        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingBook));
        when(bookRepository.existsByTitleAndAuthor_Id("New Title", author.getId())).thenReturn(true);

        // When & Then
        assertThrows(BookAlreadyExistsException.class, () -> bookService.updateBook(1L, updateRequest));
        verify(bookRepository).existsByTitleAndAuthor_Id("New Title", author.getId());
    }

    @Test
    void updateBook_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        BookResponseDto result = bookService.updateBook(1L, bookRequestDto);
        assertEquals("Test Book", result.getTitle());
        verify(bookAvailabilityService).notifyAvailabilityChange(any(Book.class), eq("UPDATED"));
    }

    // ----------------------------------------------------------------
    // decreaseAvailableCount tests
    // ----------------------------------------------------------------

    @Test
    void decreaseAvailableCount_Success() {
        book.setAvailableCount(1);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        bookService.decreaseAvailableCount(1L);
        assertEquals(0, book.getAvailableCount());
        assertEquals(BookStatus.UNAVAILABLE, book.getStatus());
        verify(stockChangeCounter).increment(-1);
    }

    @Test
    void decreaseAvailableCount_NotFound_Throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BookNotFoundException.class, () -> bookService.decreaseAvailableCount(1L));
    }

    @Test
    void decreaseAvailableCount_NotAvailable_Throws() {
        book.setAvailableCount(0);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        assertThrows(BookNotAvailableException.class, () -> bookService.decreaseAvailableCount(1L));
    }

    // ----------------------------------------------------------------
    // increaseAvailableCount tests
    // ----------------------------------------------------------------

    @Test
    void increaseAvailableCount_Success() {
        book.setAvailableCount(5);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        bookService.increaseAvailableCount(1L);
        assertEquals(6, book.getAvailableCount());
        verify(stockChangeCounter).increment(1);
    }

    @Test
    void increaseAvailableCount_NotFound_Throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BookNotFoundException.class, () -> bookService.increaseAvailableCount(1L));
    }

    @Test
    void increaseAvailableCount_ExceedsStock_Throws() {
        book.setAvailableCount(book.getStock());
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        assertThrows(BookStockException.class, () -> bookService.increaseAvailableCount(1L));
    }

    // ----------------------------------------------------------------
    // getBookEntity tests
    // ----------------------------------------------------------------

    @Test
    void getBookEntity_NotFound_Throws() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(BookNotFoundException.class, () -> bookService.getBookEntity(1L));
    }

    @Test
    void getBookEntity_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        Book result = bookService.getBookEntity(1L);
        assertEquals(1L, result.getId());
    }

    // ----------------------------------------------------------------
    // convertToResponseDto tests
    // ----------------------------------------------------------------

    @Test
    void convertToResponseDto_AuthorNull_Throws() {
        book.setAuthor(null);
        when(bookRepository.findByIdWithAuthor(anyLong())).thenReturn(book);
        assertThrows(IllegalStateException.class, () -> bookService.getBookById(1L));
        verify(bookRepository).findByIdWithAuthor(1L);
    }

}
