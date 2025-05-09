package org.pehlivan.mert.librarymanagementsystem.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorResponseDto;
import org.pehlivan.mert.librarymanagementsystem.model.book.Author;
import org.pehlivan.mert.librarymanagementsystem.repository.author.AuthorRepository;
import org.pehlivan.mert.librarymanagementsystem.service.author.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthorServiceIntegrationTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private AuthorRepository authorRepository;

    private AuthorRequestDto authorRequestDto;

    @BeforeEach
    void setUp() {
        authorRequestDto = AuthorRequestDto.builder()
                .name("Test")
                .surname("Author")
                .build();
    }

    @Test
    void createAuthor_Success() {
        // When
        AuthorResponseDto createdAuthor = authorService.createAuthor(authorRequestDto);

        // Then
        assertNotNull(createdAuthor);
        assertNotNull(createdAuthor.getId());
        assertEquals(authorRequestDto.getName(), createdAuthor.getName());
        assertEquals(authorRequestDto.getSurname(), createdAuthor.getSurname());
    }

    @Test
    void updateAuthor_Success() {
        // Given
        AuthorResponseDto createdAuthor = authorService.createAuthor(authorRequestDto);
        Author author = authorRepository.findById(createdAuthor.getId()).orElseThrow();
        author.setBooks(new ArrayList<>()); // Initialize books list

        AuthorRequestDto updateRequest = AuthorRequestDto.builder()
                .name("Updated")
                .surname("Author")
                .build();

        // When
        AuthorResponseDto updatedAuthor = authorService.updateAuthor(createdAuthor.getId(), updateRequest);

        // Then
        assertNotNull(updatedAuthor);
        assertEquals(createdAuthor.getId(), updatedAuthor.getId());
        assertEquals(updateRequest.getName(), updatedAuthor.getName());
        assertEquals(updateRequest.getSurname(), updatedAuthor.getSurname());
    }

    @Test
    void getAuthor_Success() {
        // Given
        AuthorResponseDto createdAuthor = authorService.createAuthor(authorRequestDto);

        // When
        AuthorResponseDto foundAuthor = authorService.getAuthor(createdAuthor.getId());

        // Then
        assertNotNull(foundAuthor);
        assertEquals(createdAuthor.getId(), foundAuthor.getId());
        assertEquals(createdAuthor.getName(), foundAuthor.getName());
        assertEquals(createdAuthor.getSurname(), foundAuthor.getSurname());
    }

    @Test
    void getAllAuthors_Success() {
        // Given
        authorService.createAuthor(authorRequestDto);
        AuthorRequestDto secondAuthor = AuthorRequestDto.builder()
                .name("Second")
                .surname("Author")
                .build();
        authorService.createAuthor(secondAuthor);

        // When
        var authors = authorService.getAllAuthors();

        // Then
        assertNotNull(authors);
        assertTrue(authors.size() >= 2);
    }

    @Test
    void deleteAuthor_Success() {
        // Given
        AuthorResponseDto createdAuthor = authorService.createAuthor(authorRequestDto);

        // When
        authorService.deleteAuthor(createdAuthor.getId());

        // Then
        assertFalse(authorRepository.existsById(createdAuthor.getId()));
    }
}