package org.pehlivan.mert.librarymanagementsystem;


import lombok.RequiredArgsConstructor;
import org.pehlivan.mert.librarymanagementsystem.model.book.Author;
import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.author.AuthorRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.book.BookRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.text.SimpleDateFormat;
import java.util.List;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@RequiredArgsConstructor
public class LibraryManagementSystemApplication {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementSystemApplication.class, args);
    }


	 @Bean
	public CommandLineRunner initData() {
		return args -> {
			// Create LIBRARIAN user
			User librarian = User.builder()
					.username("librarian")
					.password(passwordEncoder.encode("librarian123"))
					.email("librarian@library.com")
					.name("Library")
					.roles(List.of(Role.LIBRARIAN))
					.build();
			userRepository.save(librarian);

			// Create authors
			Author author1 = Author.builder()
					.name("George")
					.surname("Orwell")
					.build();
			authorRepository.save(author1);

			Author author2 = Author.builder()
					.name("J.R.R.")
					.surname("Tolkien")
					.build();
			authorRepository.save(author2);

			// Create books
			Book book1 = Book.builder()
					.title("1984")
					.isbn("9780451524935")
					.stock(5)
					.availableCount(5)
					.pageCount(328)
					.publicationDate(new SimpleDateFormat("yyyy-MM-dd").parse("2025-01-01"))
					.publisher("Signet Classic")
					.status(BookStatus.AVAILABLE)
					.bookType(BookType.SCIENCE_FICTION)
					.author(author1)
					.build();
			bookRepository.save(book1);

			Book book2 = Book.builder()
					.title("Animal Farm")
					.isbn("9780451526342")
					.stock(3)
					.availableCount(3)
					.pageCount(112)
					.publicationDate(new SimpleDateFormat("yyyy-MM-dd").parse("2025-01-01"))
					.publisher("Signet Classic")
					.status(BookStatus.AVAILABLE)
					.bookType(BookType.FICTION)
					.author(author1)
					.build();
			bookRepository.save(book2);

			Book book3 = Book.builder()
					.title("The Hobbit")
					.isbn("9780547928227")
					.stock(4)
					.availableCount(4)
					.pageCount(310)
					.publicationDate(new SimpleDateFormat("yyyy-MM-dd").parse("2025-01-01"))
					.publisher("Houghton Mifflin Harcourt")
					.status(BookStatus.AVAILABLE)
					.bookType(BookType.FANTASY)
					.author(author2)
					.build();
			bookRepository.save(book3);

			Book book4 = Book.builder()
					.title("The Lord of the Rings")
					.isbn("9780618640157")
					.stock(6)
					.availableCount(6)
					.pageCount(1178)
					.publicationDate(new SimpleDateFormat("yyyy-MM-dd").parse("2025-01-01"))
					.publisher("Houghton Mifflin Harcourt")
					.status(BookStatus.AVAILABLE)
					.bookType(BookType.FANTASY)
					.author(author2)
					.build();
			bookRepository.save(book4);
		};
	}
}
