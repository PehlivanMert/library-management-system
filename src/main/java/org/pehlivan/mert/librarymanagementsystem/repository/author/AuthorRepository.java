package org.pehlivan.mert.librarymanagementsystem.repository.author;

import org.pehlivan.mert.librarymanagementsystem.model.book.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByNameAndSurname(String name, String surname);
    boolean existsByNameAndSurname(String name, String surname);
} 