package org.pehlivan.mert.librarymanagementsystem.service.book;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pehlivan.mert.librarymanagementsystem.event.BookAvailabilityEvent;
import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookAvailabilityService {
    private final Sinks.Many<BookAvailabilityEvent> bookAvailabilitySink = 
        Sinks.many().multicast().onBackpressureBuffer();

    public void notifyAvailabilityChange(Book book, String eventType) {
        BookAvailabilityEvent event = BookAvailabilityEvent.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .available(book.getAvailableCount() > 0)
                .timestamp(LocalDateTime.now())
                .eventType(eventType)
                .build();

        bookAvailabilitySink.tryEmitNext(event);
        log.info("Book availability event emitted: {}", event);
    }

    public Flux<BookAvailabilityEvent> getAvailabilityStream() {
        return bookAvailabilitySink.asFlux();
    }

    public Flux<BookAvailabilityEvent> getAvailabilityStreamForBook(Long bookId) {
        return bookAvailabilitySink.asFlux()
                .filter(event -> event.getBookId().equals(bookId));
    }
} 