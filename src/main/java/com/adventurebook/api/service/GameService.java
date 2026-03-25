package com.adventurebook.api.service;

import java.time.Instant;
import java.util.List;

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
import com.adventurebook.api.model.GameEvent;
import com.adventurebook.api.model.GameSession;
import com.adventurebook.api.model.Option;
import com.adventurebook.api.model.Player;
import com.adventurebook.api.model.Section;
import com.adventurebook.api.model.SectionType;
import com.adventurebook.api.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameSessionRepository gameSessionRepository;
    private final BookService bookService;
    private final PlayerService playerService;
    private final GameResponseMapper gameResponseMapper;

    @Transactional
    public GameStateResponse startGame(Long playerId, Long bookId) {
        final Player player = playerService.findById(playerId);
        final Book book = bookService.findById(bookId);

        final Section beginSection = book.getSections().stream()
                .filter(s -> s.getType() == SectionType.BEGIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Book has no BEGIN section"));

        GameSession session = GameSession.builder()
                .player(player)
                .book(book)
                .currentSectionId(beginSection.getId())
                .health(GameSession.MAX_HEALTH)
                .alive(true)
                .finished(false)
                .paused(false)
                .lastPlayedAt(Instant.now())
                .build();

        session = gameSessionRepository.save(session);

        return gameResponseMapper.toGameStateResponse(session, beginSection, null);
    }

    public GameStateResponse getGameState(Long sessionId) {
        final GameSession session = findSession(sessionId);
        final Section currentSection = findSection(session);

        return gameResponseMapper.toGameStateResponse(session, currentSection, null);
    }

    @Transactional
    public GameStateResponse chooseOption(Long sessionId, int optionIndex) {
        final GameSession session = findSession(sessionId);

        if (session.isPaused()) {
            throw new GamePausedException(sessionId);
        }
        if (session.isDead()) {
            throw new GameOverException("Player is dead.");
        }
        if (session.isFinished()) {
            throw new GameOverException("Adventure is already finished.");
        }

        final Section currentSection = findSection(session);
        final List<Option> options = currentSection.getOptions();

        if (optionIndex < 0 || optionIndex >= options.size()) {
            throw new InvalidChoiceException(optionIndex, options.size());
        }

        final Option chosen = options.get(optionIndex);
        final int healthBefore = session.getHealth();
        GameStateResponse.ConsequenceApplied consequenceApplied = null;

        // Apply consequence if present
        if (chosen.getConsequence() != null && chosen.getConsequence().getType() != null) {
            final Consequence consequence = chosen.getConsequence();

            final int healthChange = consequence.getType() == ConsequenceType.LOSE_HEALTH
                ? consequence.getValue() * -1
                : consequence.getValue();

            session.applyHealthChange(healthChange);

            consequenceApplied = GameStateResponse.ConsequenceApplied
                .builder()
                .type(consequence.getType())
                .healthChange(healthChange)
                .healthBefore(healthBefore)
                .healthAfter(session.getHealth())
                .message(consequence.getText())
                .build();
        }

        final Section nextSection;
        final boolean playerDied = session.isDead();

        if (playerDied) {
            // player died so stay at current section
            nextSection = currentSection;
        } else {
            // go to the next section
            nextSection = findSectionById(session, chosen.getGotoId());
            session.setCurrentSectionId(nextSection.getId());

            if (nextSection.getType() == SectionType.END) {
                session.setFinished(true);
            }
        }

        final GameEvent event = GameEvent.builder()
                                         .session(session)
                                         .turnNumber(session.getEvents().size() + 1)
                                         .fromSectionId(currentSection.getId())
                                         .toSectionId(playerDied ? currentSection.getId() : nextSection.getId())
                                         .choiceDescription(chosen.getDescription())
                                         .consequenceType(consequenceApplied != null ? consequenceApplied.getType() : null)
                                         .healthChange(consequenceApplied != null ? consequenceApplied.getHealthChange() : null)
                                         .consequenceText(consequenceApplied != null ? consequenceApplied.getMessage() : null)
                                         .healthBefore(healthBefore)
                                         .healthAfter(session.getHealth())
                                         .playerDied(playerDied)
                                         .timestamp(Instant.now())
                                         .build();

        session.getEvents().add(event);

        session.setLastPlayedAt(Instant.now());
        gameSessionRepository.save(session);

        return gameResponseMapper.toGameStateResponse(session, nextSection, consequenceApplied);
    }

    @Transactional
    public GameStateResponse pauseGame(Long sessionId) {
        final GameSession session = findSession(sessionId);

        if (session.isDead()) {
            throw new GameOverException("Cannot pause: player is dead.");
        }
        if (session.isFinished()) {
            throw new GameOverException("Cannot pause: adventure is already finished.");
        }
        if (session.isPaused()) {
            throw new GameOverException("Game is already paused.");
        }

        session.setPaused(true);
        session.setLastPlayedAt(Instant.now());
        gameSessionRepository.save(session);

        final Section currentSection = findSection(session);
        return gameResponseMapper.toGameStateResponse(session, currentSection, null);
    }

    @Transactional
    public GameStateResponse resumeGame(Long sessionId) {
        final GameSession session = findSession(sessionId);

        if (session.isPaused()) {
            session.setPaused(false);
            session.setLastPlayedAt(Instant.now());
            gameSessionRepository.save(session);
        }

        final Section currentSection = findSection(session);
        return gameResponseMapper.toGameStateResponse(session, currentSection, null);
    }

    public PlayerSessionsResponse getPlayerSessions(Long playerId) {
        final Player player = playerService.findById(playerId);
        final List<GameSession> sessions = gameSessionRepository.findByPlayerIdOrderByLastPlayedAtDesc(playerId);

        return gameResponseMapper.toPlayerSessionsResponse(player, sessions);
    }

    public GameHistoryResponse getGameHistory(Long sessionId) {
        final GameSession session = findSession(sessionId);
        return gameResponseMapper.toGameHistoryResponse(session);
    }

    private GameSession findSession(Long sessionId) {
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
    }

    private Section findSection(GameSession session) {
        return findSectionById(session, session.getCurrentSectionId());
    }

    private Section findSectionById(GameSession session, int sectionId) {
        return session.getBook().getSections().stream()
                .filter(s -> s.getId() == sectionId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Section " + sectionId + " not found in book"));
    }

}
