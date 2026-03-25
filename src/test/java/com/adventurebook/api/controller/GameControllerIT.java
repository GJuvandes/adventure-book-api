package com.adventurebook.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GameControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private String playerId;
    private String bookId;

    private static final String BOOK_WITH_CONSEQUENCES = """
        {
            "title": "Danger Quest",
            "author": "Test Author",
            "difficulty": "HARD",
            "categories": ["ADVENTURE"],
            "sections": [
                {
                    "id": 1,
                    "text": "You stand at the entrance of a cave.",
                    "type": "BEGIN",
                    "options": [
                        {"description": "Enter carefully", "gotoId": 2},
                        {
                            "description": "Rush in recklessly",
                            "gotoId": 2,
                            "consequence": {"type": "LOSE_HEALTH", "value": 3, "text": "You trip and fall!"}
                        }
                    ]
                },
                {
                    "id": 2,
                    "text": "You find a healing spring.",
                    "type": "NODE",
                    "options": [
                        {
                            "description": "Drink from the spring",
                            "gotoId": 3,
                            "consequence": {"type": "GAIN_HEALTH", "value": 2, "text": "You feel refreshed!"}
                        },
                        {"description": "Ignore it and move on", "gotoId": 3}
                    ]
                },
                {
                    "id": 3,
                    "text": "You escaped the cave. The end!",
                    "type": "END",
                    "options": []
                }
            ]
        }
        """;

    @BeforeEach
    void setUp() throws Exception {
        String playerResponse = mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Max\"}"))
            .andReturn().getResponse().getContentAsString();
        playerId = com.jayway.jsonpath.JsonPath.read(playerResponse, "$.id").toString();

        String bookResponse = mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BOOK_WITH_CONSEQUENCES))
            .andReturn().getResponse().getContentAsString();
        bookId = com.jayway.jsonpath.JsonPath.read(bookResponse, "$.book.id").toString();
    }

    @Test
    void startGame_returnsInitialState() throws Exception {
        mockMvc.perform(post("/api/v1/games/start/{bookId}/player/{playerId}", bookId, playerId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.playerName", is("Max")))
            .andExpect(jsonPath("$.bookTitle", is("Danger Quest")))
            .andExpect(jsonPath("$.health", is(10)))
            .andExpect(jsonPath("$.alive", is(true)))
            .andExpect(jsonPath("$.finished", is(false)))
            .andExpect(jsonPath("$.currentSectionId", is(1)))
            .andExpect(jsonPath("$.sectionType", is("BEGIN")))
            .andExpect(jsonPath("$.options", hasSize(2)));
    }

    @Test
    void startGame_invalidPlayer_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/games/start/{bookId}/player/{playerId}", bookId, 999))
            .andExpect(status().isNotFound());
    }

    @Test
    void startGame_invalidBook_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/games/start/{bookId}/player/{playerId}", 999, playerId))
            .andExpect(status().isNotFound());
    }

    @Test
    void getGameState_returnsCurrentState() throws Exception {
        String startResponse = mockMvc.perform(
                post("/api/v1/games/start/{bookId}/player/{playerId}", bookId, playerId))
            .andReturn().getResponse().getContentAsString();
        String sessionId = com.jayway.jsonpath.JsonPath.read(startResponse, "$.sessionId").toString();

        mockMvc.perform(get("/api/v1/games/{sessionId}", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentSectionId", is(1)))
            .andExpect(jsonPath("$.health", is(10)));
    }

    @Test
    void getGameState_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/games/{sessionId}", 999))
            .andExpect(status().isNotFound());
    }

    @Test
    void chooseOption_movesToNextSection() throws Exception {
        String sessionId = startGame();

        // Choose option 0 (no consequence)
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId)
                .param("option", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentSectionId", is(2)))
            .andExpect(jsonPath("$.health", is(10)))
            .andExpect(jsonPath("$.consequenceApplied").doesNotExist());
    }

    @Test
    void chooseOption_withConsequence_appliesDamage() throws Exception {
        String sessionId = startGame();

        // Choose option 1: "Rush in recklessly" (loses 3 health)
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId)
                .param("option", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentSectionId", is(2)))
            .andExpect(jsonPath("$.health", is(7)))
            .andExpect(jsonPath("$.consequenceApplied.type", is("LOSE_HEALTH")))
            .andExpect(jsonPath("$.consequenceApplied.healthChange", is(-3)))
            .andExpect(jsonPath("$.consequenceApplied.message", is("You trip and fall!")));
    }

    @Test
    void chooseOption_reachingEnd_finishesGame() throws Exception {
        String sessionId = startGame();

        // Section 1 -> Section 2
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId)
                .param("option", "0"))
            .andExpect(status().isOk());

        // Section 2 -> Section 3 (END)
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId)
                .param("option", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentSectionId", is(3)))
            .andExpect(jsonPath("$.sectionType", is("END")))
            .andExpect(jsonPath("$.finished", is(true)));
    }

    @Test
    void chooseOption_invalidIndex_returnsBadRequest() throws Exception {
        String sessionId = startGame();

        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId)
                .param("option", "99"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void chooseOption_onFinishedGame_returnsBadRequest() throws Exception {
        String sessionId = startGame();

        // Play through to END
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId).param("option", "0"));
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId).param("option", "1"));

        // Try to choose again
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId)
                .param("option", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void pauseAndResume() throws Exception {
        String sessionId = startGame();

        mockMvc.perform(post("/api/v1/games/{sessionId}/pause", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paused", is(true)));

        // Cannot choose while paused
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId)
                .param("option", "0"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/games/{sessionId}/resume", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paused", is(false)));

        // Can choose again after resume
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId)
                .param("option", "0"))
            .andExpect(status().isOk());
    }

    @Test
    void getGameHistory_returnsEvents() throws Exception {
        String sessionId = startGame();

        // Make two choices
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId).param("option", "0"));
        mockMvc.perform(post("/api/v1/games/{sessionId}/choose", sessionId).param("option", "1"));

        mockMvc.perform(get("/api/v1/games/{sessionId}/history", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.turns", hasSize(2)))
            .andExpect(jsonPath("$.turns[0].turn", is(1)))
            .andExpect(jsonPath("$.turns[1].turn", is(2)));
    }

    @Test
    void getPlayerSessions_returnsSessions() throws Exception {
        startGame();
        startGame();

        mockMvc.perform(get("/api/v1/games/player/{playerId}", playerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessions", hasSize(2)));
    }

    private String startGame() throws Exception {
        String response = mockMvc.perform(
                post("/api/v1/games/start/{bookId}/player/{playerId}", bookId, playerId))
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.sessionId").toString();
    }
}
