package org.berrycrush.samples.graphql.controller;

import org.berrycrush.samples.graphql.model.Author;
import org.berrycrush.samples.graphql.model.Book;
import org.berrycrush.samples.graphql.repository.AuthorRepository;
import org.berrycrush.samples.graphql.repository.BookRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class BookController {
    
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    
    public BookController(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }
    
    // Queries
    
    @QueryMapping
    public Book bookById(@Argument Long id) {
        return bookRepository.findById(id).orElse(null);
    }
    
    @QueryMapping
    public Book bookByIsbn(@Argument String isbn) {
        return bookRepository.findByIsbn(isbn).orElse(null);
    }
    
    @QueryMapping
    public List<Book> books() {
        return bookRepository.findAll();
    }
    
    @QueryMapping
    public List<Book> searchBooks(@Argument String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }
    
    @QueryMapping
    public Author authorById(@Argument Long id) {
        return authorRepository.findById(id).orElse(null);
    }
    
    @QueryMapping
    public List<Author> authors() {
        return authorRepository.findAll();
    }
    
    // Mutations
    
    @MutationMapping
    public Author createAuthor(@Argument String firstName, @Argument String lastName) {
        Author author = new Author(firstName, lastName);
        return authorRepository.save(author);
    }
    
    @MutationMapping
    public Book createBook(
            @Argument String title,
            @Argument String isbn,
            @Argument Integer pageCount,
            @Argument Long authorId) {
        Book book = new Book(title, isbn, pageCount);
        if (authorId != null) {
            authorRepository.findById(authorId).ifPresent(book::setAuthor);
        }
        return bookRepository.save(book);
    }
    
    @MutationMapping
    public Book updateBook(
            @Argument Long id,
            @Argument String title,
            @Argument Integer pageCount) {
        return bookRepository.findById(id)
            .map(book -> {
                if (title != null) book.setTitle(title);
                if (pageCount != null) book.setPageCount(pageCount);
                return bookRepository.save(book);
            })
            .orElse(null);
    }
    
    @MutationMapping
    public Boolean deleteBook(@Argument Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    // Field resolvers
    
    @SchemaMapping
    public Author author(Book book) {
        return book.getAuthor();
    }
    
    @SchemaMapping
    public List<Book> books(Author author) {
        return bookRepository.findByAuthorId(author.getId());
    }
    
    @SchemaMapping
    public String fullName(Author author) {
        return author.getFullName();
    }
}
