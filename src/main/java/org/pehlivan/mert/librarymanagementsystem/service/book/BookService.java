package org.pehlivan.mert.librarymanagementsystem.service.book;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookSearchCriteriaDTO;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookAlreadyExistsException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotAvailableException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.exception.book.BookStockException;
import org.pehlivan.mert.librarymanagementsystem.model.book.Author;
import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.repository.author.AuthorRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.book.BookRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.book.BookSpecification;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "book", cacheManager = "redisCacheManager")
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final ModelMapper modelMapper;
    private final BookAvailabilityService bookAvailabilityService;
    private final MeterRegistry meterRegistry;

    private Counter totalBooksCounter;
    private Counter categoryBooksCounter;
    private Counter stockChangeCounter;

    @PostConstruct
    public void init() {
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.typeMap(Book.class, BookResponseDto.class)
            .addMappings(mapper -> {
                mapper.map(src -> src.getAuthor().getId(), BookResponseDto::setAuthorId);
                mapper.map(src -> src.getAuthor().getName(), BookResponseDto::setAuthorName);
                mapper.map(src -> src.getAuthor().getSurname(), BookResponseDto::setAuthorSurname);
            });

        totalBooksCounter = Counter.builder("library_books_total")
                .description("Total number of books")
                .register(meterRegistry);

        categoryBooksCounter = Counter.builder("library_books_category")
                .description("Number of books by category")
                .register(meterRegistry);

        stockChangeCounter = Counter.builder("library_books_stock_change")
                .description("Changes in book stock")
                .register(meterRegistry);
    }

    @Transactional
    @CacheEvict(key = "{'all', 'id:' + #result.id}")
    public BookResponseDto createBook(BookRequestDto bookRequestDto) {
        log.info("Creating new book: {}", bookRequestDto.getTitle());
        
        if (bookRepository.existsByIsbn(bookRequestDto.getIsbn())) {
            log.error("Book with ISBN {} already exists", bookRequestDto.getIsbn());
            throw new BookAlreadyExistsException("Book with ISBN " + bookRequestDto.getIsbn() + " already exists");
        }

        if (bookRequestDto.getStock() < 0) {
            log.error("Invalid stock value: {}", bookRequestDto.getStock());
            throw new BookStockException("Stock cannot be negative");
        }

        Author author = authorRepository.findByNameAndSurname(bookRequestDto.getAuthorName(), bookRequestDto.getAuthorSurname())
                .orElseGet(() -> {
                    Author newAuthor = Author.builder()
                            .name(bookRequestDto.getAuthorName())
                            .surname(bookRequestDto.getAuthorSurname())
                            .build();
                    return authorRepository.save(newAuthor);
                });

        if (bookRepository.existsByTitleAndAuthor_Id(bookRequestDto.getTitle(), author.getId())) {
            log.error("Book with title {} by author {} {} already exists", 
                    bookRequestDto.getTitle(), author.getName(), author.getSurname());
            throw new BookAlreadyExistsException("Book with title " + bookRequestDto.getTitle() + 
                    " by author " + author.getName() + " " + author.getSurname() + " already exists");
        }

        Book book = Book.builder()
                .title(bookRequestDto.getTitle())
                .isbn(bookRequestDto.getIsbn())
                .stock(bookRequestDto.getStock())
                .availableCount(bookRequestDto.getStock())
                .pageCount(bookRequestDto.getPageCount())
                .publicationDate(bookRequestDto.getPublicationDate())
                .publisher(bookRequestDto.getPublisher())
                .status(BookStatus.AVAILABLE)
                .bookType(bookRequestDto.getBookType())
                .author(author)
                .build();

        Book savedBook = bookRepository.save(book);
        bookAvailabilityService.notifyAvailabilityChange(savedBook, "CREATED");
        
        totalBooksCounter.increment();
        categoryBooksCounter.increment();
        stockChangeCounter.increment(bookRequestDto.getStock());
        
        log.info("Book created successfully with id: {}", savedBook.getId());
        return convertToResponseDto(savedBook);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'all'", unless = "#result.isEmpty()")
    public List<BookResponseDto> getAllBooks() {
        log.info("Fetching all books");
        return bookRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'id:' + #id", unless = "#result == null")
    public BookResponseDto getBookById(Long id) {
        log.info("Fetching book with id: {}", id);
        Book book = bookRepository.findByIdWithAuthor(id);
        
        if (book == null) {
            log.error("Book not found with id: {}", id);
            throw new BookNotFoundException("Book not found with id: " + id);
        }
        
        if (book.getAuthor() == null) {
            log.error("Book author is null for book: {}", book);
            throw new IllegalStateException("Book author cannot be null");
        }
        
        return convertToResponseDto(book);
    }

    @Transactional(readOnly = true)
    public Page<BookResponseDto> searchBooks(BookSearchCriteriaDTO criteria, Pageable pageable) {
        log.info("Entering pageable searchBooks method with criteria: {} and pageable: {}", criteria, pageable);
        Specification<Book> spec = BookSpecification.withSearchCriteria(criteria);
        Page<Book> books = bookRepository.findAll(spec, pageable);
        return books.map(this::convertToResponseDto);
    }

    @Transactional(readOnly = true)
    @CacheEvict(key = "{'id:' + #id, 'all'}")
    public void deleteBook(Long id) {
        log.info("Deleting book with id: {}", id);
        if (!bookRepository.existsById(id)) {
            log.error("Book not found with id: {}", id);
            throw new BookNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
        log.info("Book deleted successfully with id: {}", id);
    }

    @Transactional
    @CacheEvict(key = "{'id:' + #id, 'all'}")
    public BookResponseDto updateBook(Long id, BookRequestDto bookRequestDto) {
        log.info("Updating book with id: {}", id);
        
        if (id == null) {
            log.error("Book ID cannot be null");
            throw new IllegalArgumentException("Book ID cannot be null");
        }
        
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Book not found with id: {}", id);
                    return new BookNotFoundException("Book not found with id: " + id);
                });

        if (!existingBook.getIsbn().equals(bookRequestDto.getIsbn()) && 
            bookRepository.existsByIsbn(bookRequestDto.getIsbn())) {
            log.error("Book with ISBN {} already exists", bookRequestDto.getIsbn());
            throw new BookAlreadyExistsException("Book with ISBN " + bookRequestDto.getIsbn() + " already exists");
        }

        if (bookRequestDto.getStock() < 0) {
            log.error("Invalid stock value: {}", bookRequestDto.getStock());
            throw new BookStockException("Stock cannot be negative");
        }

        Author author = authorRepository.findByNameAndSurname(bookRequestDto.getAuthorName(), bookRequestDto.getAuthorSurname())
                .orElseGet(() -> {
                    Author newAuthor = Author.builder()
                            .name(bookRequestDto.getAuthorName())
                            .surname(bookRequestDto.getAuthorSurname())
                            .build();
                    return authorRepository.save(newAuthor);
                });

        if (!existingBook.getTitle().equals(bookRequestDto.getTitle()) || 
            !existingBook.getAuthor().getId().equals(author.getId())) {
            if (bookRepository.existsByTitleAndAuthor_Id(bookRequestDto.getTitle(), author.getId())) {
                log.error("Book with title {} by author {} {} already exists", 
                        bookRequestDto.getTitle(), author.getName(), author.getSurname());
                throw new BookAlreadyExistsException("Book with title " + bookRequestDto.getTitle() + 
                        " by author " + author.getName() + " " + author.getSurname() + " already exists");
            }
        }

        existingBook.setTitle(bookRequestDto.getTitle());
        existingBook.setIsbn(bookRequestDto.getIsbn());
        existingBook.setStock(bookRequestDto.getStock());
        existingBook.setPageCount(bookRequestDto.getPageCount());
        existingBook.setPublicationDate(bookRequestDto.getPublicationDate());
        existingBook.setPublisher(bookRequestDto.getPublisher());
        existingBook.setBookType(bookRequestDto.getBookType());
        existingBook.setAuthor(author);

        Book updatedBook = bookRepository.save(existingBook);
        bookAvailabilityService.notifyAvailabilityChange(updatedBook, "UPDATED");
        log.info("Book updated successfully: {}", updatedBook);
        return convertToResponseDto(updatedBook);
    }

    @Transactional
    @CacheEvict(key = "'id:' + #id")
    public void decreaseAvailableCount(Long id) {
        log.info("Decreasing available count for book: {}", id);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
        
        if (book.getAvailableCount() <= 0) {
            throw new BookNotAvailableException("Book is not available for loan");
        }
        
        book.setAvailableCount(book.getAvailableCount() - 1);
        book.setStatus(book.getAvailableCount() == 0 ? BookStatus.UNAVAILABLE : BookStatus.AVAILABLE);
        Book updatedBook = bookRepository.save(book);
        bookAvailabilityService.notifyAvailabilityChange(updatedBook, "BORROWED");
        
        stockChangeCounter.increment(-1);
    }

    @Transactional
    @CacheEvict(key = "'id:' + #id")
    public void increaseAvailableCount(Long id) {
        log.info("Increasing available count for book: {}", id);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
        
        if (book.getAvailableCount() >= book.getStock()) {
            throw new BookStockException("Available count cannot exceed stock");
        }
        
        book.setAvailableCount(book.getAvailableCount() + 1);
        book.setStatus(BookStatus.AVAILABLE);
        Book updatedBook = bookRepository.save(book);
        bookAvailabilityService.notifyAvailabilityChange(updatedBook, "RETURNED");
        
        stockChangeCounter.increment(1);
    }

    public Book getBookEntity(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
    }

    private BookResponseDto convertToResponseDto(Book book) {
        log.info("Converting book to response DTO: {}", book);
        if (book.getAuthor() == null) {
            log.error("Book author is null for book: {}", book);
            throw new IllegalStateException("Book author cannot be null");
        }
        
        BookResponseDto dto = new BookResponseDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setIsbn(book.getIsbn());
        dto.setStock(book.getStock());
        dto.setAvailableCount(book.getAvailableCount());
        dto.setPageCount(book.getPageCount());
        dto.setPublicationDate(book.getPublicationDate());
        dto.setPublisher(book.getPublisher());
        dto.setStatus(book.getStatus());
        dto.setBookType(book.getBookType());
        
        Author author = book.getAuthor();
        dto.setAuthorId(author.getId());
        dto.setAuthorName(author.getName());
        dto.setAuthorSurname(author.getSurname());
        
        log.info("Converted book to DTO: {}", dto);
        return dto;
    }
} 