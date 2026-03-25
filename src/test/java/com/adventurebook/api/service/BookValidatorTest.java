package com.adventurebook.api.service;

import com.adventurebook.api.dto.ValidationResult;
import com.adventurebook.api.model.Book;
import com.adventurebook.api.model.Consequence;
import com.adventurebook.api.model.ConsequenceType;
import com.adventurebook.api.model.Difficulty;
import com.adventurebook.api.model.Option;
import com.adventurebook.api.model.Section;
import com.adventurebook.api.model.SectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookValidatorTest {

    private BookValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BookValidator();
    }

    @Test
    void validBook_noErrors() {
        Book book = buildValidBook();
        ValidationResult result = validator.validate(book);

        assertFalse(result.hasErrors());
    }

    @Test
    void missingTitle_hasError() {
        Book book = buildValidBook();
        book.setTitle(null);

        ValidationResult result = validator.validate(book);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("title")));
    }

    @Test
    void missingAuthor_hasError() {
        Book book = buildValidBook();
        book.setAuthor(null);

        ValidationResult result = validator.validate(book);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("author")));
    }

    @Test
    void noBeginSection_hasError() {
        Book book = buildValidBook();
        // Replace BEGIN with NODE
        book.getSections().getFirst().setType(SectionType.NODE);

        ValidationResult result = validator.validate(book);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("BEGIN")));
    }

    @Test
    void multipleBeginSections_hasError() {
        Book book = buildValidBook();
        // Add a second BEGIN section
        Section extra = new Section();
        extra.setId(99);
        extra.setType(SectionType.BEGIN);
        extra.setText("Another beginning");
        extra.setOptions(List.of(buildOption("go", 2)));
        book.getSections().add(extra);

        ValidationResult result = validator.validate(book);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Book can't have multiple BEGIN sections")));
    }

    @Test
    void noEndSection_hasError() {
        Book book = buildValidBook();
        // Replace END with NODE that has an option back to BEGIN
        Section endSection = book.getSections().stream()
                .filter(s -> s.getType() == SectionType.END)
                .findFirst().orElseThrow();

        endSection.setType(SectionType.NODE);
        endSection.setOptions(List.of(buildOption("loop back", 1)));

        ValidationResult result = validator.validate(book);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("END")));
    }

    @Test
    void invalidGotoId_hasError() {
        Book book = buildValidBook();
        // Point to a nonexistent section
        book.getSections().getFirst().getOptions().getFirst().setGotoId(999);

        ValidationResult result = validator.validate(book);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("invalid section id")));
    }

    @Test
    void nonEndSectionWithoutOptions_hasError() {
        Book book = buildValidBook();
        // Remove options from a NODE section
        Section node = book.getSections().stream()
                .filter(s -> s.getType() == SectionType.NODE)
                .findFirst().orElse(null);

        if (node != null) {
            node.setOptions(new ArrayList<>());
        }

        ValidationResult result = validator.validate(book);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("at least one option")));
    }

    @Test
    void endNotReachableFromBegin_hasError() {
        Book book = buildValidBook();
        // Make BEGIN point to itself (no path to END)
        book.getSections().getFirst().getOptions().clear();
        book.getSections().getFirst().getOptions().add(buildOption("loop", 1));

        ValidationResult result = validator.validate(book);

        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("No END section is reachable")));
    }


    private Book buildValidBook() {
        Book book = new Book();
        book.setTitle("Test Book");
        book.setAuthor("Test Author");
        book.setDifficulty(Difficulty.EASY);

        Section begin = new Section();
        begin.setId(1);
        begin.setType(SectionType.BEGIN);
        begin.setText("The beginning");
        begin.setBook(book);
        begin.setOptions(new ArrayList<>(List.of(buildOption("go to node", 2))));

        Section node = new Section();
        node.setId(2);
        node.setType(SectionType.NODE);
        node.setText("A middle section");
        node.setBook(book);
        node.setOptions(new ArrayList<>(List.of(buildOption("go to end", 3))));

        Section end = new Section();
        end.setId(3);
        end.setType(SectionType.END);
        end.setText("The end");
        end.setBook(book);
        end.setOptions(new ArrayList<>());

        book.setSections(new ArrayList<>(List.of(begin, node, end)));
        return book;
    }

    private Option buildOption(String description, int gotoId) {
        Option option = new Option();
        option.setDescription(description);
        option.setGotoId(gotoId);
        return option;
    }

}
