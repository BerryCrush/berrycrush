package org.berrycrush.samples.graphql.repository;

import org.berrycrush.samples.graphql.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<Author> findByLastNameContainingIgnoreCase(String lastName);
}
