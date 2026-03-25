package com.adventurebook.api.controller;

import com.adventurebook.api.dto.GameHistoryResponse;
import com.adventurebook.api.dto.GameStateResponse;
import com.adventurebook.api.dto.PlayerSessionsResponse;
import com.adventurebook.api.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/start/{bookId}")
    public ResponseEntity<GameStateResponse> startGame(
        @PathVariable Long bookId,
        @RequestParam Long playerId
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(gameService.startGame(playerId, bookId));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<GameStateResponse> getGameState(@PathVariable Long sessionId) {
        return ResponseEntity.ok(gameService.getGameState(sessionId));
    }

    @PostMapping("/{sessionId}/choose")
    public ResponseEntity<GameStateResponse> chooseOption(
        @PathVariable Long sessionId,
        @RequestParam int option
    ) {
        return ResponseEntity.ok(gameService.chooseOption(sessionId, option));
    }

    @PostMapping("/{sessionId}/pause")
    public ResponseEntity<GameStateResponse> pauseGame(@PathVariable Long sessionId) {
        return ResponseEntity.ok(gameService.pauseGame(sessionId));
    }

    @PostMapping("/{sessionId}/resume")
    public ResponseEntity<GameStateResponse> resumeGame(@PathVariable Long sessionId) {
        return ResponseEntity.ok(gameService.resumeGame(sessionId));
    }

    @GetMapping("/{sessionId}/history")
    public ResponseEntity<GameHistoryResponse> getGameHistory(@PathVariable Long sessionId) {
        return ResponseEntity.ok(gameService.getGameHistory(sessionId));
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<PlayerSessionsResponse> getPlayerSessions(@PathVariable Long playerId) {
        return ResponseEntity.ok(gameService.getPlayerSessions(playerId));
    }

}
