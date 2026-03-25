package com.adventurebook.api.service;

import com.adventurebook.api.dto.ValidationResult;
import com.adventurebook.api.model.Book;
import com.adventurebook.api.model.Option;
import com.adventurebook.api.model.Section;
import com.adventurebook.api.model.SectionType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BookValidator {

    public ValidationResult validate(Book book) {
        final ValidationResult result = new ValidationResult();

        validateBookProperties(book, result);
        collectUnknownPropertyWarnings(book, result);

        if (book.getSections() == null || book.getSections().isEmpty()) {
            result.addError("Book must have at least one section.");
            return result;
        }

        validateBeginnings(book, result);
        validateEndings(book, result);
        validateGotoIds(book, result);
        validateNonEndingSectionsHaveOptions(book, result);
        validateEndReachableFromBegin(book, result);
        warnUnreachableSections(book, result);
        warnDuplicateSectionIds(book, result);

        return result;
    }

    private void validateBookProperties(Book book, ValidationResult result) {
        if (book.getTitle() == null || book.getTitle().isBlank()) {
            result.addError("Book title must not be blank.");
        }
        if (book.getAuthor() == null || book.getAuthor().isBlank()) {
            result.addError("Book author must not be blank.");
        }
    }

    private void collectUnknownPropertyWarnings(Book book, ValidationResult result) {
        if (book.getUnknownProperties() != null) {
            for (String key : book.getUnknownProperties().keySet()) {
                result.addWarning("Unknown property '" + key + "' on book will be ignored.");
            }
        }

        if (book.getSections() == null) {
            return;
        }

        for (Section section : book.getSections()) {
            if (section.getUnknownProperties() != null) {
                for (String key : section.getUnknownProperties().keySet()) {
                    result.addWarning("Unknown property '" + key + "' on section " + section.getId() + " will be ignored.");
                }
            }

            if (section.getOptions() == null) {
                continue;
            }

            for (Option option : section.getOptions()) {
                if (option.getUnknownProperties() != null) {
                    for (String key : option.getUnknownProperties().keySet()) {
                        result.addWarning("Unknown property '" + key + "' on option '" + option.getDescription() + "' in section " + section.getId() + " will be ignored.");
                    }
                }
            }
        }
    }

    private void validateBeginnings(Book book, ValidationResult result) {
        final long beginCount = book.getSections().stream()
                .filter(s -> s.getType() == SectionType.BEGIN)
                .count();

        if (beginCount == 0) {
            result.addError("Book doesn't even have a BEGIN section.");
        } else if (beginCount > 1) {
            result.addError("Book can't have multiple BEGIN sections.");
        }
    }

    private void validateEndings(Book book, ValidationResult result) {
        final long endCount = book.getSections().stream()
                .filter(s -> s.getType() == SectionType.END)
                .count();

        if (endCount == 0) {
            result.addError("Book must have at least one END section.");
        }
    }

    private void validateGotoIds(Book book, ValidationResult result) {
        final Set<Integer> validSectionIds = book.getSections().stream()
                .map(Section::getId)
                .collect(Collectors.toSet());

        for (Section section : book.getSections()) {
            if (section.getOptions() == null) {
                continue;
            }
            for (Option option : section.getOptions()) {
                if (!validSectionIds.contains(option.getGotoId())) {
                    result.addError("Section " + section.getId() + " has option pointing to invalid section id: " + option.getGotoId());
                }
            }
        }
    }

    private void validateNonEndingSectionsHaveOptions(Book book, ValidationResult result) {
        for (Section section : book.getSections()) {
            if (section.getType() != SectionType.END) {
                if (section.getOptions() == null || section.getOptions().isEmpty()) {
                    result.addError("Non-ending section " + section.getId() + " must have at least one option.");
                }
            }
        }
    }

    private void validateEndReachableFromBegin(Book book, ValidationResult result) {
        // Find the BEGIN section skip if missing, already caught by other validation
        final Section begin = book.getSections().stream()
                .filter(s -> s.getType() == SectionType.BEGIN)
                .findFirst()
                .orElse(null);

        if (begin == null) {
            return;
        }

        // Map sectionId -> list of gotoIds
        final Map<Integer, List<Integer>> nextSectionIdsBySectionId = new HashMap<>();
        for (Section section : book.getSections()) {
            if (section.getOptions() != null) {
                nextSectionIdsBySectionId.put(
                    section.getId(),
                    section.getOptions().stream()
                           .map(Option::getGotoId)
                           .toList()
                );
            } else {
                nextSectionIdsBySectionId.put(section.getId(), List.of());
            }
        }

        // Collect all END section IDs
        final Set<Integer> endIds = book.getSections().stream()
                .filter(s -> s.getType() == SectionType.END)
                .map(Section::getId)
                .collect(Collectors.toSet());

        if (endIds.isEmpty()) {
            return; // Already caught by other validation
        }

        // BFS from BEGIN
        final Set<Integer> visited = new HashSet<>();
        final Queue<Integer> queue = new LinkedList<>();

        queue.add(begin.getId());
        visited.add(begin.getId());

        while (!queue.isEmpty()) {
            final int current = queue.poll();

            if (endIds.contains(current)) {
                return; // At least one END is reachable
            }

            final List<Integer> neighbors = nextSectionIdsBySectionId.getOrDefault(current, List.of());
            for (int neighbor : neighbors) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        result.addError("No END section is reachable from the BEGIN section.");
    }

    private void warnUnreachableSections(Book book, ValidationResult result) {
        // Collect all section IDs that are referenced by any option's gotoId
        final Set<Integer> referencedIds = new HashSet<>();
        for (Section section : book.getSections()) {
            if (section.getOptions() == null) {
                continue;
            }

            for (Option option : section.getOptions()) {
                referencedIds.add(option.getGotoId());
            }
        }

        for (Section section : book.getSections()) {
            if (section.getType() == SectionType.BEGIN) {
                continue;
            }

            if (!referencedIds.contains(section.getId())) {
                result.addWarning("Section " + section.getId() + " is unreachable: no option points to it.");
            }
        }
    }

    private void warnDuplicateSectionIds(Book book, ValidationResult result) {
        final Set<Integer> seen = new HashSet<>();
        for (Section section : book.getSections()) {
            if (!seen.add(section.getId())) {
                result.addWarning("Duplicate section id " + section.getId() + " found. This may cause unexpected behavior.");
            }
        }
    }

}
