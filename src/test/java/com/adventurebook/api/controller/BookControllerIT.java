package com.adventurebook.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_BOOK = """
        {
            "title": "The Dark Forest",
            "author": "Jane Doe",
            "difficulty": "MEDIUM",
            "categories": ["ADVENTURE", "FANTASY"],
            "sections": [
                {
                    "id": 1,
                    "text": "You enter a dark forest. Two paths diverge.",
                    "type": "BEGIN",
                    "options": [
                        {"description": "Take the left path", "gotoId": 2},
                        {"description": "Take the right path", "gotoId": 3}
                    ]
                },
                {
                    "id": 2,
                    "text": "You find a treasure chest!",
                    "type": "END",
                    "options": []
                },
                {
                    "id": 3,
                    "text": "A wolf attacks you!",
                    "type": "END",
                    "options": []
                }
            ]
        }
        """;

    @Test
    void createBook_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BOOK))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.book.id", notNullValue()))
            .andExpect(jsonPath("$.book.title", is("The Dark Forest")))
            .andExpect(jsonPath("$.book.author", is("Jane Doe")))
            .andExpect(jsonPath("$.book.difficulty", is("MEDIUM")));
    }

    @Test
    void createBook_invalidNoBeginSection_returnsBadRequest() throws Exception {
        String invalidBook = """
            {
                "title": "Bad Book",
                "author": "Author",
                "difficulty": "EASY",
                "categories": ["FICTION"],
                "sections": [
                    {
                        "id": 1,
                        "text": "The end.",
                        "type": "END",
                        "options": []
                    }
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBook))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getBooks_returnsAll() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BOOK))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void getBooks_filterByDifficulty() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BOOK))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/books").param("difficulty", "MEDIUM"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/books").param("difficulty", "HARD"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getBook_returnsBook() throws Exception {
        String response = mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BOOK))
            .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(response, "$.book.id").toString();

        mockMvc.perform(get("/api/v1/books/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("The Dark Forest")))
            .andExpect(jsonPath("$.sections", hasSize(3)));
    }

    @Test
    void getBook_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", 999))
            .andExpect(status().isNotFound());
    }

    @Test
    void addAndRemoveCategories() throws Exception {
        String response = mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BOOK))
            .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(response, "$.book.id").toString();

        mockMvc.perform(put("/api/v1/books/{id}/categories", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"HORROR\", \"MYSTERY\"]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories", hasSize(4)))
            .andExpect(jsonPath("$.categories", containsInAnyOrder("ADVENTURE", "FANTASY", "HORROR", "MYSTERY")));

        mockMvc.perform(delete("/api/v1/books/{id}/categories/{category}", id, "HORROR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories", hasSize(3)));
    }
}
