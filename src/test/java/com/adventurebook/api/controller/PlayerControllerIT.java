package com.adventurebook.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlayerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerPlayer_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Lucas\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is("Lucas")))
            .andExpect(jsonPath("$.id", notNullValue()));
    }

    @Test
    void registerPlayer_duplicateName_returnsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Lucas\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Lucas\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void registerPlayer_blankName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listPlayers_returnsAll() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Lucas\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Bob\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/players"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)));
    }

    @Test
    void getPlayer_returnsPlayer() throws Exception {
        String response = mockMvc.perform(post("/api/v1/players")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Lucas\"}"))
            .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id").toString();

        mockMvc.perform(get("/api/v1/players/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Lucas")));
    }

    @Test
    void getPlayer_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/players/{id}", 999))
            .andExpect(status().isNotFound());
    }
}
