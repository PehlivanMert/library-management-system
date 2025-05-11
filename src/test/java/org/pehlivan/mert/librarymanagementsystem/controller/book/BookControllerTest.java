package org.pehlivan.mert.librarymanagementsystem.controller.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookSearchCriteriaDTO;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;
import org.pehlivan.mert.librarymanagementsystem.service.book.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    private BookRequestDto bookRequestDto;
    private BookResponseDto bookResponseDto;

    @BeforeEach
    void setUp() {
        bookRequestDto = new BookRequestDto();
        bookRequestDto.setTitle("Test Book");
        bookRequestDto.setIsbn("9781234567890");
        bookRequestDto.setStock(5);
        bookRequestDto.setPageCount(200);
        bookRequestDto.setPublicationDate(new Date());
        bookRequestDto.setPublisher("Test Publisher");
        bookRequestDto.setBookType(BookType.FICTION);
        bookRequestDto.setAuthorName("John");
        bookRequestDto.setAuthorSurname("Doe");

        bookResponseDto = new BookResponseDto();
        bookResponseDto.setId(1L);
        bookResponseDto.setTitle("Test Book");
        bookResponseDto.setIsbn("9781234567890");
        bookResponseDto.setStock(5);
        bookResponseDto.setAvailableCount(5);
        bookResponseDto.setPageCount(200);
        bookResponseDto.setPublicationDate(new Date());
        bookResponseDto.setPublisher("Test Publisher");
        bookResponseDto.setStatus(BookStatus.AVAILABLE);
        bookResponseDto.setBookType(BookType.FICTION);
        bookResponseDto.setAuthorId(1L);
        bookResponseDto.setAuthorName("John");
        bookResponseDto.setAuthorSurname("Doe");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void createBook_ShouldReturnCreatedBook() throws Exception {
        when(bookService.createBook(any(BookRequestDto.class))).thenReturn(bookResponseDto);

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.isbn").value("9781234567890"));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN", "READER"})
    void getAllBooks_ShouldReturnBookList() throws Exception {
        List<BookResponseDto> books = Arrays.asList(bookResponseDto);
        when(bookService.getAllBooks()).thenReturn(books);

        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Book"));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN", "READER"})
    void getBookById_ShouldReturnBook() throws Exception {
        when(bookService.getBookById(anyLong())).thenReturn(bookResponseDto);

        mockMvc.perform(get("/api/v1/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void updateBook_ShouldReturnUpdatedBook() throws Exception {
        when(bookService.updateBook(anyLong(), any(BookRequestDto.class))).thenReturn(bookResponseDto);

        mockMvc.perform(put("/api/v1/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void deleteBook_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/books/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN", "READER"})
    void searchBooks_ShouldReturnPagedResults() throws Exception {
        List<BookResponseDto> books = Arrays.asList(bookResponseDto);
        Page<BookResponseDto> bookPage = new PageImpl<>(books);
        when(bookService.searchBooks(any(BookSearchCriteriaDTO.class), any(Pageable.class))).thenReturn(bookPage);

        mockMvc.perform(get("/api/v1/books/search")
                .param("title", "Test")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Test Book"));
    }

    @Test
    @WithMockUser(roles = "READER")
    void createBook_WithReaderRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookRequestDto)))
                .andExpect(status().isForbidden());
    }
} 