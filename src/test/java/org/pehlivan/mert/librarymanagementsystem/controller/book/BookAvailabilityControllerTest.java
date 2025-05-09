package org.pehlivan.mert.librarymanagementsystem.controller.book;

import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.book.BookAvailabilityEvent;
import org.pehlivan.mert.librarymanagementsystem.service.book.BookAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookAvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookAvailabilityService bookAvailabilityService;

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void streamAllBookAvailability_ShouldReturnEventStream() throws Exception {
        BookAvailabilityEvent event = BookAvailabilityEvent.builder()
                .bookId(1L)
                .bookTitle("Test Book")
                .available(true)
                .timestamp(LocalDateTime.now())
                .eventType("UPDATED")
                .build();

        when(bookAvailabilityService.getAvailabilityStream())
                .thenReturn(Flux.just(event));

        mockMvc.perform(get("/api/v1/books/availability")
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void streamBookAvailability_ShouldReturnEventStream() throws Exception {
        BookAvailabilityEvent event = BookAvailabilityEvent.builder()
                .bookId(1L)
                .bookTitle("Test Book")
                .available(true)
                .timestamp(LocalDateTime.now())
                .eventType("UPDATED")
                .build();

        when(bookAvailabilityService.getAvailabilityStreamForBook(anyLong()))
                .thenReturn(Flux.just(event));

        mockMvc.perform(get("/api/v1/books/availability/1")
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "READER")
    void streamAllBookAvailability_WithReaderRole_ShouldReturnEventStream() throws Exception {
        BookAvailabilityEvent event = BookAvailabilityEvent.builder()
                .bookId(1L)
                .bookTitle("Test Book")
                .available(true)
                .timestamp(LocalDateTime.now())
                .eventType("UPDATED")
                .build();

        when(bookAvailabilityService.getAvailabilityStream())
                .thenReturn(Flux.just(event));

        mockMvc.perform(get("/api/v1/books/availability")
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "READER")
    void streamBookAvailability_WithReaderRole_ShouldReturnEventStream() throws Exception {
        BookAvailabilityEvent event = BookAvailabilityEvent.builder()
                .bookId(1L)
                .bookTitle("Test Book")
                .available(true)
                .timestamp(LocalDateTime.now())
                .eventType("UPDATED")
                .build();

        when(bookAvailabilityService.getAvailabilityStreamForBook(anyLong()))
                .thenReturn(Flux.just(event));

        mockMvc.perform(get("/api/v1/books/availability/1")
                .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
    }
} 