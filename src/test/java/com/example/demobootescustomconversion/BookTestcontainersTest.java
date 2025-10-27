package com.example.demobootescustomconversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class BookTestcontainersTest {

    @Container
    static ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false");

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
    }

    @Test
    void testFindById() {
        // Given - Create and save a book
        Book book = new Book();
        book.setTitle("Elasticsearch Handbook");
        book.setAuthor("Jane Smith");
        book.setPublicationYear(2023);
        book.setPrice(39.99);

        Book savedBook = bookRepository.save(book);
        String bookId = savedBook.getId();

        // When - Find the book by ID
        Optional<Book> foundBook = bookRepository.findById(bookId);

        // Then - Verify the book was found
        assertThat(foundBook).isPresent();
        assertThat(foundBook.get().getTitle()).isEqualTo("Elasticsearch Handbook");
        assertThat(foundBook.get().getAuthor()).isEqualTo("Jane Smith");
    }

    @Test
    void testFindByAuthor() {
        // Given - Create and save multiple books by the same author
        Book book1 = new Book();
        book1.setTitle("Java Fundamentals");
        book1.setAuthor("Robert Martin");
        book1.setPublicationYear(2022);
        book1.setPrice(34.99);

        Book book2 = new Book();
        book2.setTitle("Clean Code");
        book2.setAuthor("Robert Martin");
        book2.setPublicationYear(2023);
        book2.setPrice(45.99);

        bookRepository.save(book1);
        bookRepository.save(book2);

        // When - Find books by author
        List<Book> books = bookRepository.findByAuthor("Robert Martin");

        // Then - Verify all books are found
        assertThat(books).hasSize(2);
        assertThat(books).extracting(Book::getTitle).containsOnly("Java Fundamentals", "Clean Code");
    }

    @Test
    void testDeleteBook() {
        // Given - Create and save a book
        Book book = new Book();
        book.setTitle("Advanced Spring");
        book.setAuthor("Mark Johnson");
        book.setPublicationYear(2024);
        book.setPrice(49.99);

        Book savedBook = bookRepository.save(book);
        String bookId = savedBook.getId();

        // When - Delete the book
        bookRepository.delete(savedBook);

        // Then - Verify the book was deleted
        Optional<Book> deletedBook = bookRepository.findById(bookId);
        assertThat(deletedBook).isEmpty();
    }

    @Test
    void testCreateRetrieveAndDeleteWorkflow() {
        // Given - Create multiple books
        Book book1 = new Book();
        book1.setTitle("Database Design");
        book1.setAuthor("Alan Turing");
        book1.setPublicationYear(2022);
        book1.setPrice(52.99);

        Book book2 = new Book();
        book2.setTitle("System Architecture");
        book2.setAuthor("Grace Hopper");
        book2.setPublicationYear(2023);
        book2.setPrice(58.99);

        // Create - Save books
        Book savedBook1 = bookRepository.save(book1);
        Book savedBook2 = bookRepository.save(book2);

        // Retrieve - Find by ID
        Optional<Book> foundBook1 = bookRepository.findById(savedBook1.getId());
        assertThat(foundBook1).isPresent();
        assertThat(foundBook1.get().getTitle()).isEqualTo("Database Design");

        // Retrieve - Find all
        Iterable<Book> allBooks = bookRepository.findAll();
        assertThat(allBooks).hasSize(2);

        // Delete - Remove one book
        bookRepository.delete(savedBook1);
        Optional<Book> deletedBook = bookRepository.findById(savedBook1.getId());
        assertThat(deletedBook).isEmpty();

        // Verify remaining book still exists
        Optional<Book> remainingBook = bookRepository.findById(savedBook2.getId());
        assertThat(remainingBook).isPresent();
        assertThat(remainingBook.get().getTitle()).isEqualTo("System Architecture");
    }
}
