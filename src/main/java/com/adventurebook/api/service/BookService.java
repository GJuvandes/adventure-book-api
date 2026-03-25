package com.adventurebook.api.service;

import com.adventurebook.api.dto.BookCreateResponse;
import com.adventurebook.api.dto.ValidationResult;
import com.adventurebook.api.exception.BookNotFoundException;
import com.adventurebook.api.exception.InvalidBookException;
import com.adventurebook.api.model.Book;
import com.adventurebook.api.model.Category;
import com.adventurebook.api.model.Difficulty;
import com.adventurebook.api.model.Option;
import com.adventurebook.api.model.Section;
import com.adventurebook.api.repository.BookRepository;
import com.adventurebook.api.repository.BookSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookValidator bookValidator;

    public List<Book> findBooks(String title, String author, Category category, Difficulty difficulty) {
        Specification<Book> spec = Specification.where(null);

        if (title != null && !title.isBlank()) {
            spec = spec.and(BookSpecifications.titleContains(title));
        }

        if (author != null && !author.isBlank()) {
            spec = spec.and(BookSpecifications.authorContains(author));
        }

        if (category != null) {
            spec = spec.and(BookSpecifications.hasCategory(category));
        }

        if (difficulty != null) {
            spec = spec.and(BookSpecifications.hasDifficulty(difficulty));
        }

        return bookRepository.findAll(spec);
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public Book addCategories(Long id, Set<Category> categories) {
        final Book book = findById(id);
        book.getCategories().addAll(categories);
        return bookRepository.save(book);
    }

    @Transactional
    public Book removeCategory(Long id, Category category) {
        final Book book = findById(id);
        book.getCategories().remove(category);
        return bookRepository.save(book);
    }

    @Transactional
    public BookCreateResponse createBook(Book book) {
        // Set up bidirectional relationships
        if (book.getSections() != null) {
            for (Section section : book.getSections()) {
                section.setBook(book);
                if (section.getOptions() != null) {
                    for (Option option : section.getOptions()) {
                        option.setSection(section);
                    }
                }
            }
        }

        // Validate
        final ValidationResult result = bookValidator.validate(book);
        if (result.hasErrors()) {
            throw new InvalidBookException(result.getErrors(), result.getWarnings());
        }

        final Book saved = bookRepository.save(book);

        return BookCreateResponse.builder()
                .book(saved)
                .warnings(result.hasWarnings() ? result.getWarnings() : null)
                .build();
    }

}
