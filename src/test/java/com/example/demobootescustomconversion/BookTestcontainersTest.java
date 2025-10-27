package com.example.demobootescustomconversion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class BookTestcontainersTest {

    @Container
    static ElasticsearchContainer elasticsearchContainer = new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:8.18.2")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false");

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

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

    @Test
    void testSaveAndRetrieveBookWithDataNanos() {
        // Given - Create a book with dataNanos (6-digit microsecond precision)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        LocalDateTime timestamp = LocalDateTime.of(2024, 10, 27, 14, 30, 45, 123456000);
        String timestampString = timestamp.format(formatter);

        Book book = new Book();
        book.setTitle("Time Series Database");
        book.setAuthor("Data Engineer");
        book.setPublicationYear(2024);
        book.setPrice(59.99);
        book.setDate(timestampString);

        // When - Save the book
        Book savedBook = bookRepository.save(book);
        String bookId = savedBook.getId();

        // Then - Retrieve and verify the nanosecond precision
        Optional<Book> foundBook = bookRepository.findById(bookId);
        assertThat(foundBook).isPresent();
        assertThat(foundBook.get().getDate()).isNotNull();
        String expectedDateNanos = "2024-10-27T14:30:45.123456";
        assertThat(foundBook.get().getDate())
                .isEqualTo(timestampString)
                .isEqualTo(expectedDateNanos);

        // Parse and verify the LocalDateTime contains nanosecond precision (microsecond level - 6 digits)
        LocalDateTime retrievedTimestamp = LocalDateTime.parse(foundBook.get().getDate(), formatter);
        assertThat(retrievedTimestamp.getYear()).isEqualTo(2024);
        assertThat(retrievedTimestamp.getMonthValue()).isEqualTo(10);
        assertThat(retrievedTimestamp.getDayOfMonth()).isEqualTo(27);
        assertThat(retrievedTimestamp.getHour()).isEqualTo(14);
        assertThat(retrievedTimestamp.getMinute()).isEqualTo(30);
        assertThat(retrievedTimestamp.getSecond()).isEqualTo(45);

        // Verify microsecond precision (nanosecond divided by 1000 to get microseconds)
        int nanoOfSecond = retrievedTimestamp.getNano();
        int microseconds = nanoOfSecond / 1000;
        assertThat(microseconds).isEqualTo(123456); // 6 digits of microseconds
    }

    @Test
    void testDataNanosWithFullPrecision() {
        // Given - Create a book with precise microsecond data (6 digits)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        LocalDateTime preciseTimestamp = LocalDateTime.of(2024, 12, 25, 23, 59, 59, 999999000);
        String timestampString = preciseTimestamp.format(formatter);

        Book book = new Book();
        book.setTitle("Precision Time Book");
        book.setAuthor("Time Master");
        book.setPublicationYear(2024);
        book.setPrice(79.99);
        book.setDate(timestampString);

        // When - Save and retrieve
        Book savedBook = bookRepository.save(book);
        Optional<Book> foundBook = bookRepository.findById(savedBook.getId());

        // Then - Verify precision is maintained
        assertThat(foundBook).isPresent();
        String retrievedString = foundBook.get().getDate();
        String expectedDateNanos = "2024-12-25T23:59:59.999999";
        assertThat(retrievedString)
                .isNotNull()
                .isEqualTo(timestampString)
                .isEqualTo(expectedDateNanos);

        // Parse and verify
        LocalDateTime retrieved = LocalDateTime.parse(retrievedString, formatter);
        int nanoOfSecond = retrieved.getNano();
        int microseconds = nanoOfSecond / 1000;
        assertThat(microseconds).isEqualTo(999999); // Full 6-digit microsecond precision
    }

    @Test
    void testDateFieldSupportsRangeQuery() throws InterruptedException {
        // Given - Create books with different timestamps
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 6, 15, 15, 30, 0, 0);
        LocalDateTime time3 = LocalDateTime.of(2024, 12, 31, 23, 59, 0, 0);

        Book book1 = new Book();
        book1.setTitle("Early Year Book");
        book1.setDate(time1.format(formatter));

        Book book2 = new Book();
        book2.setTitle("Mid Year Book");
        book2.setDate(time2.format(formatter));

        Book book3 = new Book();
        book3.setTitle("Late Year Book");
        book3.setDate(time3.format(formatter));

        bookRepository.save(book1);
        bookRepository.save(book2);
        bookRepository.save(book3);

        // Wait for indexing
        Thread.sleep(1000);

        // When - Use Elasticsearch range query to find books within a date range
        String startDate = "2024-02-01T00:00:00.000000";
        String endDate = "2024-11-30T23:59:59.999999";

        Criteria criteria = new Criteria("date")
                .greaterThanEqual(startDate)
                .lessThanEqual(endDate);

        org.springframework.data.elasticsearch.core.query.Query rangeQuery = new CriteriaQuery(criteria);

        SearchHits<Book> searchHits = elasticsearchOperations.search(rangeQuery, Book.class);
        List<Book> books = searchHits.getSearchHits().stream()
                .map(org.springframework.data.elasticsearch.core.SearchHit::getContent)
                .toList();

        // Then - Verify only books within range are found
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("Mid Year Book");
    }

    @Test
    void testDateFieldGreaterThanQuery() throws InterruptedException {
        // Given - Create books with different timestamps
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

        LocalDateTime time1 = LocalDateTime.of(2024, 10, 27, 14, 30, 45, 50000000);
        LocalDateTime time2 = LocalDateTime.of(2024, 10, 27, 14, 30, 45, 123456000);
        LocalDateTime time3 = LocalDateTime.of(2024, 10, 27, 14, 30, 45, 789012000);

        Book book1 = new Book();
        book1.setTitle("First Book");
        book1.setDate(time1.format(formatter));

        Book book2 = new Book();
        book2.setTitle("Second Book");
        book2.setDate(time2.format(formatter));

        Book book3 = new Book();
        book3.setTitle("Third Book");
        book3.setDate(time3.format(formatter));

        bookRepository.save(book1);
        bookRepository.save(book2);
        bookRepository.save(book3);

        // Thread.sleep(1000);

        // When - Use Elasticsearch gt query to find books after base time
        String baseTime = time2.format(formatter);

        Criteria criteria = new Criteria("date").greaterThan(baseTime);

        Query gtQuery = new CriteriaQuery(criteria);

        SearchHits<Book> searchHits = elasticsearchOperations.search(gtQuery, Book.class);
        List<Book> books = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        // Then - Verify only later books are found
        assertThat(books).hasSize(1);
        assertThat(books.getFirst().getTitle()).isEqualTo("Third Book");
    }

    @Test
    void testDateFieldLessThanQuery() throws InterruptedException {
        // Given - Create books with different timestamps
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

        LocalDateTime time1 = LocalDateTime.of(2024, 6, 15, 10, 0, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 7, 20, 15, 30, 0, 0);
        LocalDateTime time3 = LocalDateTime.of(2024, 8, 25, 20, 45, 0, 0);

        Book book1 = new Book();
        book1.setTitle("June Book");
        book1.setDate(time1.format(formatter));

        Book book2 = new Book();
        book2.setTitle("July Book");
        book2.setDate(time2.format(formatter));

        Book book3 = new Book();
        book3.setTitle("August Book");
        book3.setDate(time3.format(formatter));

        bookRepository.save(book1);
        bookRepository.save(book2);
        bookRepository.save(book3);

        // Thread.sleep(1000);

        // When - Use Elasticsearch lt query to find books before base time
        String baseTime = time2.format(formatter);

        Criteria criteria = new Criteria("date").lessThan(baseTime);

        Query ltQuery = new CriteriaQuery(criteria);

        SearchHits<Book> searchHits = elasticsearchOperations.search(ltQuery, Book.class);
        List<Book> books = searchHits
                .getSearchHits()
                .stream()
                .map(SearchHit::getContent)
                .toList();

        // Then - Verify only earlier books are found
        assertThat(books).hasSize(1);
        assertThat(books.getFirst().getTitle()).isEqualTo("June Book");
    }
}
