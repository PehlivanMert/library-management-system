package org.pehlivan.mert.librarymanagementsystem.repository.book;

import org.pehlivan.mert.librarymanagementsystem.dto.book.BookSearchCriteriaDTO;
import org.pehlivan.mert.librarymanagementsystem.model.book.Book;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookStatus;
import org.pehlivan.mert.librarymanagementsystem.model.book.BookType;
import org.springframework.data.jpa.domain.Specification;

public class BookSpecification {

    public static Specification<Book> withSearchCriteria(BookSearchCriteriaDTO criteria) {
        return Specification.where(hasTitle(criteria.getTitle()))
                .and(hasAuthorName(criteria.getAuthorName()))
                .and(hasAuthorSurname(criteria.getAuthorSurname()))
                .and(hasIsbn(criteria.getIsbn()))
                .and(hasBookType(criteria.getBookType()))
                .and(hasStatus(criteria.getStatus()));
    }

    public static Specification<Book> hasTitle(String title) {
        return (root, query, criteriaBuilder) -> 
            title == null ? null : criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")), 
                "%" + title.toLowerCase() + "%"
            );
    }

    public static Specification<Book> hasAuthorName(String authorName) {
        return (root, query, criteriaBuilder) -> 
            authorName == null ? null : criteriaBuilder.like(
                criteriaBuilder.lower(root.get("author").get("name")), 
                "%" + authorName.toLowerCase() + "%"
            );
    }

    public static Specification<Book> hasAuthorSurname(String authorSurname) {
        return (root, query, criteriaBuilder) -> 
            authorSurname == null ? null : criteriaBuilder.like(
                criteriaBuilder.lower(root.get("author").get("surname")), 
                "%" + authorSurname.toLowerCase() + "%"
            );
    }

    public static Specification<Book> hasIsbn(String isbn) {
        return (root, query, criteriaBuilder) -> 
            isbn == null ? null : criteriaBuilder.equal(root.get("isbn"), isbn);
    }

    public static Specification<Book> hasBookType(BookType bookType) {
        return (root, query, criteriaBuilder) -> 
            bookType == null ? null : criteriaBuilder.equal(root.get("bookType"), bookType);
    }

    public static Specification<Book> hasStatus(BookStatus status) {
        return (root, query, criteriaBuilder) -> 
            status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }
} 