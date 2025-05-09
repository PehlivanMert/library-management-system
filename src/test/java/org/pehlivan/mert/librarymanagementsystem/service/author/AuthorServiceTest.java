package org.pehlivan.mert.librarymanagementsystem.service.author;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;

import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.author.AuthorNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.model.book.Author;
import org.pehlivan.mert.librarymanagementsystem.repository.author.AuthorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter totalAuthorsCounter;

    @Mock
    private Counter booksPerAuthorCounter;

    @Mock
    private Counter newAuthorsCounter;

    @InjectMocks
    private AuthorService authorService;

    private Author testAuthor;
    private AuthorResponseDto testAuthorDto;
    private AuthorRequestDto testAuthorRequestDto;

    @BeforeEach
    void setUp() {
        // Setup meter registry mocks
        when(meterRegistry.counter("library.authors.total")).thenReturn(totalAuthorsCounter);
        when(meterRegistry.counter("library.authors.books.per.author")).thenReturn(booksPerAuthorCounter);
        when(meterRegistry.counter("library.authors.new")).thenReturn(newAuthorsCounter);

        // Initialize test data
        testAuthor = Author.builder()
                .id(1L)
                .name("John")
                .surname("Doe")
                .build();

        testAuthorDto = AuthorResponseDto.builder()
                .name("John")
                .surname("Doe")
                .build();

        testAuthorRequestDto = AuthorRequestDto.builder()
                .name("John")
                .surname("Doe")
                .build();

        // Initialize service
        authorService.init();
    }

    @Test
    void createAuthor_Success() {
        // Arrange
        when(authorRepository.save(any(Author.class))).thenReturn(testAuthor);
        when(modelMapper.map(any(Author.class), eq(AuthorResponseDto.class))).thenReturn(testAuthorDto);

        // Act
        AuthorResponseDto result = authorService.createAuthor(testAuthorRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(testAuthorDto.getName(), result.getName());
        assertEquals(testAuthorDto.getSurname(), result.getSurname());
        verify(totalAuthorsCounter).increment();
        verify(newAuthorsCounter).increment();
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    void createAuthor_WithEmptySurname_ThrowsException() {
        // Arrange
        AuthorRequestDto invalidRequest = AuthorRequestDto.builder()
                .name("John")
                .surname("")
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authorService.createAuthor(invalidRequest));
        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    void getAuthorById_Success() {
        // Arrange
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));

        // Act
        Author result = authorService.getAuthorById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testAuthor.getName(), result.getName());
        assertEquals(testAuthor.getSurname(), result.getSurname());
    }

    @Test
    void getAuthorById_NotFound_ThrowsException() {
        // Arrange
        when(authorRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AuthorNotFoundException.class, () -> authorService.getAuthorById(1L));
    }

    @Test
    void getAuthorByNameAndSurname_Success() {
        // Arrange
        when(authorRepository.findByNameAndSurname("John", "Doe"))
                .thenReturn(Optional.of(testAuthor));

        // Act
        Author result = authorService.getAuthorByNameAndSurname("John", "Doe");

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getName());
        assertEquals("Doe", result.getSurname());
    }

    @Test
    void getAuthorByNameAndSurname_NotFound_ThrowsException() {
        // Arrange
        when(authorRepository.findByNameAndSurname("John", "Doe"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AuthorNotFoundException.class, () -> 
            authorService.getAuthorByNameAndSurname("John", "Doe"));
    }

    @Test
    void getAuthor_Success() {
        // Arrange
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(modelMapper.map(testAuthor, AuthorResponseDto.class)).thenReturn(testAuthorDto);

        // Act
        AuthorResponseDto result = authorService.getAuthor(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testAuthorDto.getName(), result.getName());
        assertEquals(testAuthorDto.getSurname(), result.getSurname());
    }

    @Test
    void getAllAuthors_Success() {
        // Arrange
        List<Author> authors = List.of(testAuthor);
        when(authorRepository.findAll()).thenReturn(authors);
        when(modelMapper.map(testAuthor, AuthorResponseDto.class)).thenReturn(testAuthorDto);

        // Act
        List<AuthorResponseDto> results = authorService.getAllAuthors();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(testAuthorDto.getName(), results.get(0).getName());
    }

    @Test
    void updateAuthor_Success() {
        // Arrange
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));
        when(authorRepository.save(any(Author.class))).thenReturn(testAuthor);
        when(modelMapper.map(any(AuthorRequestDto.class), eq(Author.class))).thenReturn(testAuthor);
        when(modelMapper.map(testAuthor, AuthorResponseDto.class)).thenReturn(testAuthorDto);

        // Act
        AuthorResponseDto result = authorService.updateAuthor(1L, testAuthorRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(testAuthorDto.getName(), result.getName());
        assertEquals(testAuthorDto.getSurname(), result.getSurname());
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    void deleteAuthor_Success() {
        // Arrange
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));

        // Act
        authorService.deleteAuthor(1L);

        // Assert
        verify(authorRepository).deleteById(1L);
    }

    @Test
    void updateAuthorBooks_Success() {
        // Arrange
        when(authorRepository.findById(1L)).thenReturn(Optional.of(testAuthor));

        // Act
        authorService.updateAuthorBooks(1L, 5);

        // Assert
        verify(booksPerAuthorCounter).increment(5);
    }
} 