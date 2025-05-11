package org.pehlivan.mert.librarymanagementsystem.util.dataInitialize;

import lombok.RequiredArgsConstructor;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.model.book.Author;
import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;
import org.pehlivan.mert.librarymanagementsystem.model.loan.Loan;
import org.pehlivan.mert.librarymanagementsystem.model.user.Role;
import org.pehlivan.mert.librarymanagementsystem.model.user.User;
import org.pehlivan.mert.librarymanagementsystem.repository.author.AuthorRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.book.BookRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.loan.LoanRepository;
import org.pehlivan.mert.librarymanagementsystem.repository.user.UserRepository;
import org.pehlivan.mert.librarymanagementsystem.service.loan.LoanService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class Data implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoanService loanService;


    // This method is called when the application starts
    // It checks if the librarian user exists in the database
    // If not, it calls the initializeData() method to populate the database with initial data

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("librarian").isEmpty()) {
            initializeData();
        }
    }


    private void initializeData() throws ParseException, InterruptedException {

        // Create LIBRARIAN user
        User librarian = User.builder().username("librarian").password(passwordEncoder.encode("librarian123")).email("librarian@library.com").name("Library").roles(List.of(Role.LIBRARIAN)).build();
        userRepository.save(librarian);

        User librarian2 = User.builder().username("librarian2").password(passwordEncoder.encode("librarian456")).email("librarian2@library.com").name("Library Two").roles(List.of(Role.LIBRARIAN)).build();
        userRepository.save(librarian2);

        // Create READER user
        User reader = User.builder().username("reader").password(passwordEncoder.encode("reader123")).email("reader@reader.com").name("Reader").roles(List.of(Role.READER)).build();
        userRepository.save(reader);

        User reader2 = User.builder().username("reader2").password(passwordEncoder.encode("reader234")).email("reader2@reader.com").name("Reader Two").roles(List.of(Role.READER)).build();
        userRepository.save(reader2);

        User reader3 = User.builder().username("reader3").password(passwordEncoder.encode("reader345")).email("reader3@reader.com").name("Reader Three").roles(List.of(Role.READER)).build();
        userRepository.save(reader3);

        User reader4 = User.builder().username("reader4").password(passwordEncoder.encode("reader456")).email("reader4@reader.com").name("Reader Four").roles(List.of(Role.READER)).build();
        userRepository.save(reader4);

        User reader5 = User.builder().username("reader5").password(passwordEncoder.encode("reader567")).email("reader5@reader.com").name("Reader Five").roles(List.of(Role.READER)).build();
        userRepository.save(reader5);


        // Create authors
        Author author1 = Author.builder().name("George").surname("Orwell").build();
        authorRepository.save(author1);

        Author author2 = Author.builder().name("J.R.R.").surname("Tolkien").build();
        authorRepository.save(author2);

        Author author3 = Author.builder().name("Jane").surname("Austen").build();
        authorRepository.save(author3);

        Author author4 = Author.builder().name("Mark").surname("Twain").build();
        authorRepository.save(author4);

        Author author5 = Author.builder().name("Fyodor").surname("Dostoevsky").build();
        authorRepository.save(author5);

        Author author6 = Author.builder().name("Leo").surname("Tolstoy").build();
        authorRepository.save(author6);

        Author author7 = Author.builder().name("Harper").surname("Lee").build();
        authorRepository.save(author7);

        Author author8 = Author.builder().name("Ernest").surname("Hemingway").build();
        authorRepository.save(author8);

        Author author9 = Author.builder().name("Agatha").surname("Christie").build();
        authorRepository.save(author9);

        Author author10 = Author.builder().name("Franz").surname("Kafka").build();
        authorRepository.save(author10);


        // Create books
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Jane Austen
        Book book1 = Book.builder().title("Pride and Prejudice").isbn("9780141439518").stock(4).availableCount(4).pageCount(279).publicationDate(sdf.parse("2025-02-01")).publisher("Penguin Classics").status(BookStatus.AVAILABLE).bookType(BookType.ROMANCE).author(author3).build();
        bookRepository.save(book1);

        Book book2 = Book.builder().title("Emma").isbn("9780141192475").stock(3).availableCount(3).pageCount(474).publicationDate(sdf.parse("2025-02-15")).publisher("Penguin Classics").status(BookStatus.AVAILABLE).bookType(BookType.DRAMA).author(author3).build();
        bookRepository.save(book2);

// Mark Twain
        Book book3 = Book.builder().title("The Adventures of Tom Sawyer").isbn("9780141321103").stock(6).availableCount(6).pageCount(274).publicationDate(sdf.parse("2025-03-01")).publisher("Oxford University Press").status(BookStatus.AVAILABLE).bookType(BookType.ACTION_AND_ADVENTURE).author(author4).build();
        bookRepository.save(book3);

        Book book4 = Book.builder().title("Adventures of Huckleberry Finn").isbn("9780486280615").stock(5).availableCount(5).pageCount(366).publicationDate(sdf.parse("2025-03-10")).publisher("Signet Classics").status(BookStatus.AVAILABLE).bookType(BookType.WESTERN).author(author4).build();
        bookRepository.save(book4);

// Fyodor Dostoevsky
        Book book5 = Book.builder().title("Crime and Punishment").isbn("9780140449136").stock(7).availableCount(7).pageCount(671).publicationDate(sdf.parse("2025-04-01")).publisher("Penguin Books").status(BookStatus.AVAILABLE).bookType(BookType.MYSTERY).author(author5).build();
        bookRepository.save(book5);

        Book book6 = Book.builder().title("The Brothers Karamazov").isbn("9780374528379").stock(4).availableCount(4).pageCount(796).publicationDate(sdf.parse("2025-04-15")).publisher("Farrar, Straus and Giroux").status(BookStatus.AVAILABLE).bookType(BookType.DRAMA).author(author5).build();
        bookRepository.save(book6);

// Leo Tolstoy
        Book book7 = Book.builder().title("War and Peace").isbn("9780140447934").stock(5).availableCount(5).pageCount(1225).publicationDate(sdf.parse("2025-05-01")).publisher("Penguin Books").status(BookStatus.AVAILABLE).bookType(BookType.HISTORICAL).author(author6).build();
        bookRepository.save(book7);

        Book book8 = Book.builder().title("Anna Karenina").isbn("9780143035008").stock(4).availableCount(4).pageCount(964).publicationDate(sdf.parse("2025-05-15")).publisher("Penguin Classics").status(BookStatus.AVAILABLE).bookType(BookType.ROMANCE).author(author6).build();
        bookRepository.save(book8);

// Harper Lee
        Book book9 = Book.builder().title("To Kill a Mockingbird").isbn("9780061120084").stock(6).availableCount(6).pageCount(336).publicationDate(sdf.parse("2025-06-01")).publisher("Harper Perennial").status(BookStatus.AVAILABLE).bookType(BookType.FICTION).author(author7).build();
        bookRepository.save(book9);

        Book book10 = Book.builder().title("Go Set a Watchman").isbn("9780062409850").stock(3).availableCount(3).pageCount(288).publicationDate(sdf.parse("2025-06-15")).publisher("HarperCollins").status(BookStatus.AVAILABLE).bookType(BookType.DRAMA).author(author7).build();
        bookRepository.save(book10);

// Ernest Hemingway
        Book book11 = Book.builder().title("The Old Man and the Sea").isbn("9780684801223").stock(5).availableCount(5).pageCount(132).publicationDate(sdf.parse("2025-07-01")).publisher("Scribner").status(BookStatus.AVAILABLE).bookType(BookType.ACTION_AND_ADVENTURE).author(author8).build();
        bookRepository.save(book11);

        Book book12 = Book.builder().title("A Farewell to Arms").isbn("9780684801469").stock(4).availableCount(4).pageCount(352).publicationDate(sdf.parse("2025-07-15")).publisher("Scribner").status(BookStatus.AVAILABLE).bookType(BookType.HISTORICAL).author(author8).build();
        bookRepository.save(book12);

// Agatha Christie
        Book book13 = Book.builder().title("Murder on the Orient Express").isbn("9780062693662").stock(6).availableCount(6).pageCount(256).publicationDate(sdf.parse("2025-08-01")).publisher("HarperCollins").status(BookStatus.AVAILABLE).bookType(BookType.MYSTERY).author(author9).build();
        bookRepository.save(book13);

        Book book14 = Book.builder().title("And Then There Were None").isbn("9780062073488").stock(5).availableCount(5).pageCount(272).publicationDate(sdf.parse("2025-08-15")).publisher("HarperCollins").status(BookStatus.AVAILABLE).bookType(BookType.THRILLER).author(author9).build();
        bookRepository.save(book14);

// Franz Kafka
        Book book15 = Book.builder().title("The Trial").isbn("9780805210408").stock(4).availableCount(4).pageCount(255).publicationDate(sdf.parse("2025-09-01")).publisher("Schocken").status(BookStatus.AVAILABLE).bookType(BookType.DRAMA).author(author10).build();
        bookRepository.save(book15);

        Book book16 = Book.builder().title("The Metamorphosis").isbn("9780553213690").stock(6).availableCount(6).pageCount(201).publicationDate(sdf.parse("2025-09-10")).publisher("Bantam Classics").status(BookStatus.AVAILABLE).bookType(BookType.POETRY).author(author10).build();
        bookRepository.save(book16);

        // George Orwell
        Book book17 = Book.builder().title("1984").isbn("9780451524935").stock(5).availableCount(5).pageCount(328).publicationDate(sdf.parse("2025-01-01")).publisher("Signet Classic").status(BookStatus.AVAILABLE).bookType(BookType.SCIENCE_FICTION).author(author1).build();
        bookRepository.save(book17);

        Book book18 = Book.builder().title("Animal Farm").isbn("9780451526342").stock(3).availableCount(3).pageCount(112).publicationDate(sdf.parse("2025-01-01")).publisher("Signet Classic").status(BookStatus.AVAILABLE).bookType(BookType.FICTION).author(author1).build();
        bookRepository.save(book18);
// J.R.R. Tolkien
        Book book19 = Book.builder().title("The Hobbit").isbn("9780547928227").stock(4).availableCount(4).pageCount(310).publicationDate(sdf.parse("2025-01-01")).publisher("Houghton Mifflin Harcourt").status(BookStatus.AVAILABLE).bookType(BookType.FANTASY).author(author2).build();
        bookRepository.save(book19);

        Book book20 = Book.builder().title("The Lord of the Rings").isbn("9780618640157").stock(6).availableCount(6).pageCount(1178).publicationDate(sdf.parse("2025-01-01")).publisher("Houghton Mifflin Harcourt").status(BookStatus.AVAILABLE).bookType(BookType.FANTASY).author(author2).build();
        bookRepository.save(book20);

        // Loans
            Thread.sleep(2000); // 2 saniye bekle
        // Loans - Borrowed today
        loanService.borrowBook(LoanRequestDto.builder().bookId(2L).userId(3L).borrowedDate(LocalDate.now()).build());
        loanService.borrowBook(LoanRequestDto.builder().bookId(4L).userId(7L).borrowedDate(LocalDate.now()).build());
        loanService.borrowBook(LoanRequestDto.builder().bookId(6L).userId(4L).borrowedDate(LocalDate.now()).build());
        loanService.borrowBook(LoanRequestDto.builder().bookId(8L).userId(5L).borrowedDate(LocalDate.now()).build());
        loanService.borrowBook(LoanRequestDto.builder().bookId(10L).userId(6L).borrowedDate(LocalDate.now()).build());
        // Loans - Borrowed 15 days ago
        loanService.borrowBook(LoanRequestDto.builder().bookId(1L).userId(3L).borrowedDate(LocalDate.now().minusDays(15)).build());
        loanService.borrowBook(LoanRequestDto.builder().bookId(3L).userId(4L).borrowedDate(LocalDate.now().minusDays(15)).build());
        loanService.borrowBook(LoanRequestDto.builder().bookId(5L).userId(5L).borrowedDate(LocalDate.now().minusDays(15)).build());
        // Loans - Borrowed 20 days ago
        loanService.borrowBook(LoanRequestDto.builder().bookId(7L).userId(5L).borrowedDate(LocalDate.now().minusDays(20)).build());
        loanService.borrowBook(LoanRequestDto.builder().bookId(9L).userId(6L).borrowedDate(LocalDate.now().minusDays(20)).build());
        // Loans - Borrowed 30 days ago
        loanService.borrowBook(LoanRequestDto.builder().bookId(11L).userId(3L).borrowedDate(LocalDate.now().minusDays(31)).build());
    }
}
