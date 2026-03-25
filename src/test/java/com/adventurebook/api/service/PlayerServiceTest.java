package com.adventurebook.api.service;

import com.adventurebook.api.exception.DuplicatePlayerNameException;
import com.adventurebook.api.exception.PlayerNotFoundException;
import com.adventurebook.api.model.Player;
import com.adventurebook.api.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private PlayerService playerService;

    private Player mike;

    @BeforeEach
    void setUp() {
        mike = Player.builder()
                     .id(1L)
                     .name("Mike")
                     .build();
    }

    @Test
    void register_success() {
        when(playerRepository.existsByName("Mike")).thenReturn(false);
        when(playerRepository.save(any(Player.class))).thenReturn(mike);

        Player result = playerService.register("Mike");

        assertNotNull(result);
        assertEquals("Mike", result.getName());
        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void register_duplicateName_throws() {
        when(playerRepository.existsByName("Mike")).thenReturn(true);

        assertThrows(DuplicatePlayerNameException.class, () -> playerService.register("Mike"));
        verify(playerRepository, never()).save(any());
    }

    @Test
    void findById_found() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(mike));

        Player result = playerService.findById(1L);

        assertEquals("Mike", result.getName());
    }

    @Test
    void findById_notFound_throws() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () -> playerService.findById(99L));
    }
}
