package com.adventurebook.api.controller;

import com.adventurebook.api.dto.PlayerRegistrationRequest;
import com.adventurebook.api.model.Player;
import com.adventurebook.api.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    public ResponseEntity<Player> register(@Valid @RequestBody PlayerRegistrationRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(playerService.register(request.getName()));
    }

    @GetMapping
    public ResponseEntity<List<Player>> listPlayers() {
        return ResponseEntity.ok(playerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.findById(id));
    }

}
