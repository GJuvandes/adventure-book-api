package com.adventurebook.api.service;

import com.adventurebook.api.exception.DuplicatePlayerNameException;
import com.adventurebook.api.exception.PlayerNotFoundException;
import com.adventurebook.api.model.Player;
import com.adventurebook.api.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    public Player register(String name) {
        if (playerRepository.existsByName(name)) {
            throw new DuplicatePlayerNameException(name);
        }

        Player player = Player.builder()
                .name(name)
                .build();

        return playerRepository.save(player);
    }

    public Player findById(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
    }

    public List<Player> findAll() {
        return playerRepository.findAll();
    }

}
