package org.pehlivan.mert.librarymanagementsystem.repository.book;

import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.author WHERE b.id = :id")
    Book findByIdWithAuthor(@Param("id") Long id);

    // EntityGraph ile pagination destekli arama - LEFT JOIN FETCH yerine
    @EntityGraph(attributePaths = {"author"})
    Page<Book> findAll(org.springframework.data.jpa.domain.Specification<Book> spec, Pageable pageable);

    // Custom query ile LEFT JOIN FETCH ve pagination
    @Query(value = "SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Book b")
    Page<Book> findAllWithAuthor(Pageable pageable);

    // Tüm kitapları LEFT JOIN FETCH ile getir (pagination destekli)
    @Query(value = "SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.author ORDER BY b.id",
           countQuery = "SELECT COUNT(DISTINCT b) FROM Book b")
    Page<Book> findAllBooksWithAuthor(Pageable pageable);

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    boolean existsByTitleAndAuthor_Id(String title, Long authorId);
}
