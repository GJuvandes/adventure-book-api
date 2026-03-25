package com.adventurebook.api.repository;

import com.adventurebook.api.model.Book;
import com.adventurebook.api.model.Category;
import com.adventurebook.api.model.Difficulty;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> titleContains(String title) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Book> authorContains(String author) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("author")), "%" + author.toLowerCase() + "%");
    }

    public static Specification<Book> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) ->
                cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Book> hasCategory(Category category) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.isMember(category, root.get("categories"));
        };
    }

}
