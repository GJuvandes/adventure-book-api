package com.adventurebook.api.controller;

import com.adventurebook.api.dto.BookCreateResponse;
import com.adventurebook.api.model.Book;
import com.adventurebook.api.model.Category;
import com.adventurebook.api.model.Difficulty;
import com.adventurebook.api.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<List<Book>> getBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Difficulty difficulty
    ) {

        List<Book> books = bookService.findBooks(title, author, category, difficulty);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBook(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.findById(id));
    }

    @PutMapping("/{id}/categories")
    public ResponseEntity<Book> addCategories(
        @PathVariable Long id,
        @RequestBody Set<Category> categories
    ) {

        return ResponseEntity.ok(bookService.addCategories(id, categories));
    }

    @DeleteMapping("/{id}/categories/{category}")
    public ResponseEntity<Book> removeCategory(
        @PathVariable Long id,
        @PathVariable Category category
    ) {
        return ResponseEntity.ok(bookService.removeCategory(id, category));
    }

    @PostMapping
    public ResponseEntity<BookCreateResponse> createBook(@RequestBody Book book) {
        BookCreateResponse response = bookService.createBook(book);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

}
