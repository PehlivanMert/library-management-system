package org.pehlivan.mert.librarymanagementsystem.controller.author;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.author.AuthorResponseDto;
import org.pehlivan.mert.librarymanagementsystem.exception.RateLimitExceededException;
import org.pehlivan.mert.librarymanagementsystem.service.author.AuthorService;
import org.pehlivan.mert.librarymanagementsystem.config.RateLimitConfig;
import org.pehlivan.mert.librarymanagementsystem.config.RateLimitInterceptor;
import org.pehlivan.mert.librarymanagementsystem.repository.author.AuthorRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.book.BookRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthorControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorService authorService;

    @MockBean
    private AuthorRepository authorRepository;

    @MockBean
    private BookRepository bookRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private Bucket bucket;

    private AuthorRequestDto authorRequestDto;
    private AuthorResponseDto authorDto;
    private ConsumptionProbe successProbe;
    private ConsumptionProbe failProbe;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        authorRequestDto = new AuthorRequestDto();
        authorRequestDto.setName("John");
        authorRequestDto.setSurname("Doe");

        authorDto = new AuthorResponseDto();
        authorDto.setId(1L);
        authorDto.setName("John");
        authorDto.setSurname("Doe");

        // Mock successful probe
        successProbe = mock(ConsumptionProbe.class);
        when(successProbe.isConsumed()).thenReturn(true);
        when(successProbe.getRemainingTokens()).thenReturn(99L);
        when(successProbe.getNanosToWaitForReset()).thenReturn(0L);

        // Mock failed probe
        failProbe = mock(ConsumptionProbe.class);
        when(failProbe.isConsumed()).thenReturn(false);
        when(failProbe.getRemainingTokens()).thenReturn(0L);
        when(failProbe.getNanosToWaitForRefill()).thenReturn(300_000_000_000L); // 5 minutes
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void createAuthor_ShouldReturnCreatedAuthor() throws Exception {
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
        when(authorService.createAuthor(any(AuthorRequestDto.class))).thenReturn(authorDto);

        mockMvc.perform(post("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authorRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.surname").value("Doe"));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN", "READER"})
    void getAllAuthors_ShouldReturnAuthorList() throws Exception {
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
        List<AuthorResponseDto> authors = Arrays.asList(authorDto);
        when(authorService.getAllAuthors()).thenReturn(authors);

        mockMvc.perform(get("/api/v1/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("John"));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN", "READER"})
    void getAuthorById_ShouldReturnAuthor() throws Exception {
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
        when(authorService.getAuthor(anyLong())).thenReturn(authorDto);

        mockMvc.perform(get("/api/v1/authors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void updateAuthor_ShouldReturnUpdatedAuthor() throws Exception {
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
        when(authorService.updateAuthor(anyLong(), any(AuthorRequestDto.class))).thenReturn(authorDto);

        mockMvc.perform(put("/api/v1/authors/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authorRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void deleteAuthor_ShouldReturnNoContent() throws Exception {
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
        doNothing().when(authorService).deleteAuthor(anyLong());

        mockMvc.perform(delete("/api/v1/authors/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "READER")
    void createAuthor_WithReaderRole_ShouldReturnForbidden() throws Exception {
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
        mockMvc.perform(post("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authorRequestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void createAuthor_RateLimitExceeded_Throws() throws Exception {
        when(authorService.createAuthor(any(AuthorRequestDto.class))).thenReturn(authorDto);

        // First 100 requests succeed
        for (int i = 0; i < 100; i++) {
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
            mockMvc.perform(post("/api/v1/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(authorRequestDto)))
                    .andExpect(status().isCreated());
        }

        // 101st request fails
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(failProbe);
        mockMvc.perform(post("/api/v1/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authorRequestDto)))
                .andExpect(status().isTooManyRequests())
                .andExpect(result -> {
                    assertTrue(result.getResolvedException() instanceof RateLimitExceededException);
                    RateLimitExceededException ex = (RateLimitExceededException) result.getResolvedException();
                    assertTrue(ex.getRemainingRequests() == 0);
                    assertTrue(ex.getResetTime() > 0);
                });
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void getAllAuthors_RateLimitExceeded_Throws() throws Exception {
        List<AuthorResponseDto> authors = Arrays.asList(authorDto);
        when(authorService.getAllAuthors()).thenReturn(authors);

        // First 100 requests succeed
        for (int i = 0; i < 100; i++) {
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
            mockMvc.perform(get("/api/v1/authors"))
                    .andExpect(status().isOk());
        }

        // 101st request fails
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(failProbe);
        mockMvc.perform(get("/api/v1/authors"))
                .andExpect(status().isTooManyRequests())
                .andExpect(result -> {
                    assertTrue(result.getResolvedException() instanceof RateLimitExceededException);
                    RateLimitExceededException ex = (RateLimitExceededException) result.getResolvedException();
                    assertTrue(ex.getRemainingRequests() == 0);
                    assertTrue(ex.getResetTime() > 0);
                });
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void getAuthorById_RateLimitExceeded_Throws() throws Exception {
        when(authorService.getAuthor(anyLong())).thenReturn(authorDto);

        // First 100 requests succeed
        for (int i = 0; i < 100; i++) {
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(successProbe);
            mockMvc.perform(get("/api/v1/authors/1"))
                    .andExpect(status().isOk());
        }

        // 101st request fails
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(failProbe);
        mockMvc.perform(get("/api/v1/authors/1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(result -> {
                    assertTrue(result.getResolvedException() instanceof RateLimitExceededException);
                    RateLimitExceededException ex = (RateLimitExceededException) result.getResolvedException();
                    assertTrue(ex.getRemainingRequests() == 0);
                    assertTrue(ex.getResetTime() > 0);
                });
    }
} 