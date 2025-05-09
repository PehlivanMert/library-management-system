package org.pehlivan.mert.librarymanagementsystem.service.author;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorRequestDto;
import org.pehlivan.mert.librarymanagementsystem.exception.author.AuthorNotFoundException;
import org.pehlivan.mert.librarymanagementsystem.model.book.Author;
import org.pehlivan.mert.librarymanagementsystem.repository.author.AuthorRepository;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "author", cacheManager = "redisCacheManager")
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final ModelMapper modelMapper;
    private final MeterRegistry meterRegistry;

    private Counter totalAuthorsCounter;
    private Counter booksPerAuthorCounter;
    private Counter newAuthorsCounter;

    @PostConstruct
    public void init() {
        totalAuthorsCounter = meterRegistry.counter("library.authors.total");
        booksPerAuthorCounter = meterRegistry.counter("library.authors.books.per.author");
        newAuthorsCounter = meterRegistry.counter("library.authors.new");
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public AuthorResponseDto createAuthor(AuthorRequestDto authorRequestDto) {
        log.info("Creating author: {}", authorRequestDto);
        if (authorRequestDto.getSurname() == null || authorRequestDto.getSurname().trim().isEmpty()) {
            throw new IllegalArgumentException("Author surname cannot be null or empty");
        }
        
        Author author = Author.builder()
                .name(authorRequestDto.getName())
                .surname(authorRequestDto.getSurname())
                .build();
        
        Author savedAuthor = authorRepository.save(author);
        
        totalAuthorsCounter.increment();
        newAuthorsCounter.increment();
        
        return modelMapper.map(savedAuthor, AuthorResponseDto.class);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'id:' + #id", unless = "#result == null")
    public Author getAuthorById(Long id) {
        log.info("Getting author by id: {}", id);
        return authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException("Author not found with id: " + id));
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'name:' + #name + ':' + #surname", unless = "#result == null")
    public Author getAuthorByNameAndSurname(String name, String surname) {
        log.info("Getting author by name: {} and surname: {}", name, surname);
        return authorRepository.findByNameAndSurname(name, surname)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AuthorNotFoundException("Author not found with name: " + name + " and surname: " + surname));
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'dto:' + #id", unless = "#result == null")
    public AuthorResponseDto getAuthor(Long id) {
        return modelMapper.map(getAuthorById(id), AuthorResponseDto.class);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'all'", unless = "#result.isEmpty()")
    public List<AuthorResponseDto> getAllAuthors() {
        log.info("Getting all authors");
        return authorRepository.findAll().stream()
                .map(author -> modelMapper.map(author, AuthorResponseDto.class))
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public AuthorResponseDto updateAuthor(Long id, AuthorRequestDto authorRequestDto) {
        log.info("Updating author with id: {}", id);
        Author existingAuthor = getAuthorById(id);

        // Yazar bilgileri değişiyorsa, ilişkili kitapları da güncelle
        if (!existingAuthor.getName().equals(authorRequestDto.getName()) || 
            !existingAuthor.getSurname().equals(authorRequestDto.getSurname())) {
            
            // Yeni isim-soyisim kombinasyonu başka bir yazara ait mi kontrol et
            if (authorRepository.existsByNameAndSurname(authorRequestDto.getName(), authorRequestDto.getSurname())) {
                throw new IllegalArgumentException("Author with name " + authorRequestDto.getName() + 
                    " and surname " + authorRequestDto.getSurname() + " already exists");
            }

            // Yazar bilgilerini güncelle
            existingAuthor.setName(authorRequestDto.getName());
            existingAuthor.setSurname(authorRequestDto.getSurname());
            
            // İlişkili kitapları güncelle
            existingAuthor.getBooks().forEach(book -> {
                book.setAuthor(existingAuthor);
            });
        }

        Author updatedAuthor = authorRepository.save(existingAuthor);
        return modelMapper.map(updatedAuthor, AuthorResponseDto.class);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public void deleteAuthor(Long id) {
        log.info("Deleting author with id: {}", id);
        authorRepository.deleteById(id);
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public void updateAuthorBooks(Long authorId, int bookCount) {
        log.info("Updating book count for author: {}", authorId);
        Author author = getAuthorById(authorId);
        booksPerAuthorCounter.increment(bookCount);
    }
} 