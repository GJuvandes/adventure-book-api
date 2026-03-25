package com.adventurebook.api.service;

import com.adventurebook.api.dto.GameHistoryResponse;
import com.adventurebook.api.dto.GameStateResponse;
import com.adventurebook.api.dto.PlayerSessionsResponse;
import com.adventurebook.api.exception.GameOverException;
import com.adventurebook.api.exception.GamePausedException;
import com.adventurebook.api.exception.GameSessionNotFoundException;
import com.adventurebook.api.exception.InvalidChoiceException;
import com.adventurebook.api.mapper.GameResponseMapper;
import com.adventurebook.api.model.Book;
import com.adventurebook.api.model.Consequence;
import com.adventurebook.api.model.ConsequenceType;
import com.adventurebook.api.model.Difficulty;
import com.adventurebook.api.model.GameEvent;
import com.adventurebook.api.model.GameSession;
import com.adventurebook.api.model.Option;
import com.adventurebook.api.model.Player;
import com.adventurebook.api.model.Section;
import com.adventurebook.api.model.SectionType;
import com.adventurebook.api.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private BookService bookService;

    @Mock
    private PlayerService playerService;

    @Spy
    private GameResponseMapper gameResponseMapper = new GameResponseMapper();

    @InjectMocks
    private GameService gameService;

    private Player player;
    private Book book;
    private Section beginSection;
    private Section nodeSection;
    private Section endSection;

    @BeforeEach
    void setUp() {
        player = Player.builder().id(1L).name("Mike").build();

        book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Author");
        book.setDifficulty(Difficulty.EASY);

        beginSection = new Section();
        beginSection.setId(1);
        beginSection.setType(SectionType.BEGIN);
        beginSection.setText("You stand at a crossroads.");
        beginSection.setBook(book);

        nodeSection = new Section();
        nodeSection.setId(2);
        nodeSection.setType(SectionType.NODE);
        nodeSection.setText("A wolf appears!");
        nodeSection.setBook(book);

        endSection = new Section();
        endSection.setId(3);
        endSection.setType(SectionType.END);
        endSection.setText("You escaped!");
        endSection.setBook(book);

        Option toNode = new Option();
        toNode.setDescription("Go left");
        toNode.setGotoId(2);

        Option toEnd = new Option();
        toEnd.setDescription("Go right");
        toEnd.setGotoId(3);

        Consequence damage = new Consequence();
        damage.setType(ConsequenceType.LOSE_HEALTH);
        damage.setValue(3);
        damage.setText("The wolf bites you.");

        Option fightWolf = new Option();
        fightWolf.setDescription("Fight the wolf");
        fightWolf.setGotoId(3);
        fightWolf.setConsequence(damage);

        beginSection.setOptions(List.of(toNode, toEnd));
        nodeSection.setOptions(List.of(fightWolf));
        endSection.setOptions(List.of());

        book.setSections(List.of(beginSection, nodeSection, endSection));
    }

    @Test
    void startGame_returnsStateAtBeginSection() {
        when(playerService.findById(1L)).thenReturn(player);
        when(bookService.findById(1L)).thenReturn(book);
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> {
            GameSession s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        GameStateResponse response = gameService.startGame(1L, 1L);

        assertNotNull(response);
        assertEquals("Mike", response.getPlayerName());
        assertEquals("Test Book", response.getBookTitle());
        assertEquals(10, response.getHealth());
        assertEquals(1, response.getCurrentSectionId());
        assertEquals(SectionType.BEGIN, response.getSectionType());
        assertTrue(response.isAlive());
        assertFalse(response.isFinished());
        assertFalse(response.isPaused());
        assertEquals(2, response.getOptions().size());
    }

    @Test
    void chooseOption_movesToNextSection() {
        GameSession session = buildActiveSession(beginSection);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Choose "Go left" (index 0) -> goes to node section (id 2)
        GameStateResponse response = gameService.chooseOption(1L, 0);

        assertEquals(2, response.getCurrentSectionId());
        assertEquals("A wolf appears!", response.getText());
        assertEquals(10, response.getHealth());
    }

    @Test
    void chooseOption_withConsequence_appliesDamage() {
        GameSession session = buildActiveSession(nodeSection);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Choose "Fight the wolf" (index 0) -> LOSE_HEALTH 3
        GameStateResponse response = gameService.chooseOption(1L, 0);

        assertEquals(7, response.getHealth());
        assertNotNull(response.getConsequenceApplied());
        assertEquals(ConsequenceType.LOSE_HEALTH, response.getConsequenceApplied().getType());
        assertEquals(-3, response.getConsequenceApplied().getHealthChange());
        assertEquals(10, response.getConsequenceApplied().getHealthBefore());
        assertEquals(7, response.getConsequenceApplied().getHealthAfter());
    }

    @Test
    void chooseOption_reachesEnd_marksFinished() {
        GameSession session = buildActiveSession(beginSection);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Choose "Go right" (index 1) -> goes to END section (id 3)
        GameStateResponse response = gameService.chooseOption(1L, 1);

        assertEquals(3, response.getCurrentSectionId());
        assertTrue(response.isFinished());
        assertEquals(SectionType.END, response.getSectionType());
    }

    @Test
    void chooseOption_lethalDamage_playerDies() {
        GameSession session = buildActiveSession(nodeSection);
        session.setHealth(2); // less than the 3 damage from wolf
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(inv -> inv.getArgument(0));

        GameStateResponse response = gameService.chooseOption(1L, 0);

        assertEquals(0, response.getHealth());
        assertFalse(response.isAlive());
        // Player stays at current section when dying
        assertEquals(2, response.getCurrentSectionId());
    }

    @Test
    void chooseOption_invalidIndex_throws() {
        GameSession session = buildActiveSession(beginSection);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(InvalidChoiceException.class, () -> gameService.chooseOption(1L, 5));
        assertThrows(InvalidChoiceException.class, () -> gameService.chooseOption(1L, -1));
    }

    @Test
    void chooseOption_onDeadPlayer_throws() {
        GameSession session = buildActiveSession(beginSection);
        session.setAlive(false);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(GameOverException.class, () -> gameService.chooseOption(1L, 0));
    }

    @Test
    void getGameState_returnsCurrentState() {
        GameSession session = buildActiveSession(beginSection);
        when(gameSessionRepository.findById(1L)).thenReturn(Optional.of(session));

        GameStateResponse response = gameService.getGameState(1L);

        assertEquals(1, response.getCurrentSectionId());
        assertEquals("You stand at a crossroads.", response.getText());
    }

    @Test
    void getGameState_sessionNotFound_throws() {
        when(gameSessionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(GameSessionNotFoundException.class, () -> gameService.getGameState(99L));
    }



    private GameSession buildActiveSession(Section currentSection) {
        GameSession session = GameSession.builder()
                .id(1L)
                .player(player)
                .book(book)
                .currentSectionId(currentSection.getId())
                .health(GameSession.MAX_HEALTH)
                .alive(true)
                .finished(false)
                .paused(false)
                .build();
        session.setEvents(new ArrayList<>());
        return session;
    }

}
