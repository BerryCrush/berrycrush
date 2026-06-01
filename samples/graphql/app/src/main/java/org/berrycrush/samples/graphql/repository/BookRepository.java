package org.berrycrush.samples.graphql.repository;

import org.berrycrush.samples.graphql.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByAuthorId(Long authorId);
    List<Book> findByTitleContainingIgnoreCase(String title);
}
