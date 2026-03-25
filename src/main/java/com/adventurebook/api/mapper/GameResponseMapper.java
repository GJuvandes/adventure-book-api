package com.adventurebook.api.mapper;

import com.adventurebook.api.dto.GameHistoryResponse;
import com.adventurebook.api.dto.GameStateResponse;
import com.adventurebook.api.dto.PlayerSessionsResponse;
import com.adventurebook.api.model.GameSession;
import com.adventurebook.api.model.Option;
import com.adventurebook.api.model.Player;
import com.adventurebook.api.model.Section;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
public class GameResponseMapper {

    public GameStateResponse toGameStateResponse(
        GameSession session,
        Section section,
        GameStateResponse.ConsequenceApplied consequenceApplied
    ) {
        List<GameStateResponse.OptionDto> optionDtos = List.of();

        if (section.getOptions() != null
            && !section.getOptions().isEmpty()
            && session.isAlive()
            && !session.isFinished()
        ) {
            optionDtos = IntStream.range(0, section.getOptions().size())
                                  .mapToObj(i -> {
                                      Option opt = section.getOptions().get(i);
                                      return GameStateResponse.OptionDto
                                          .builder()
                                          .index(i)
                                          .description(opt.getDescription())
                                          .hasConsequence(opt.getConsequence() != null
                                              && opt.getConsequence().getType() != null)
                                          .build();
                                  })
                                  .toList();
        }

        return GameStateResponse
            .builder()
            .sessionId(session.getId())
            .playerName(session.getPlayer().getName())
            .bookTitle(session.getBook().getTitle())
            .health(session.getHealth())
            .maxHealth(GameSession.MAX_HEALTH)
            .alive(session.isAlive())
            .finished(session.isFinished())
            .paused(session.isPaused())
            .currentSectionId(section.getId())
            .sectionType(section.getType())
            .text(section.getText())
            .consequenceApplied(consequenceApplied)
            .options(optionDtos)
            .build();
    }

    public GameHistoryResponse toGameHistoryResponse(GameSession session) {
        final List<GameHistoryResponse.TurnDto> turns = session.getEvents().stream()
                .map(e ->
                    GameHistoryResponse.TurnDto
                        .builder()
                        .turn(e.getTurnNumber())
                        .fromSectionId(e.getFromSectionId())
                        .toSectionId(e.getToSectionId())
                        .choice(e.getChoiceDescription())
                        .consequenceType(e.getConsequenceType())
                        .healthChange(e.getHealthChange())
                        .consequenceText(e.getConsequenceText()).healthBefore(e.getHealthBefore())
                        .healthAfter(e.getHealthAfter())
                        .playerDied(e.isPlayerDied())
                        .timestamp(e.getTimestamp())
                        .build()
                )
                .toList();

        return GameHistoryResponse
            .builder()
            .sessionId(session.getId())
            .bookTitle(session.getBook().getTitle())
            .playerName(session.getPlayer().getName())
            .finalHealth(session.getHealth())
            .maxHealth(GameSession.MAX_HEALTH)
            .alive(session.isAlive())
            .finished(session.isFinished())
            .paused(session.isPaused())
            .totalTurns(turns.size())
            .turns(turns)
            .build();
    }

    public PlayerSessionsResponse toPlayerSessionsResponse(Player player, List<GameSession> sessions) {
        final List<PlayerSessionsResponse.SessionSummary> summaries = sessions.stream()
                .map(s -> PlayerSessionsResponse.SessionSummary.builder()
                        .sessionId(s.getId())
                        .bookId(s.getBook().getId())
                        .bookTitle(s.getBook().getTitle())
                        .health(s.getHealth())
                        .maxHealth(GameSession.MAX_HEALTH)
                        .alive(s.isAlive())
                        .finished(s.isFinished())
                        .paused(s.isPaused())
                        .currentSectionId(s.getCurrentSectionId())
                        .totalTurns(s.getEvents().size())
                        .lastPlayedAt(s.getLastPlayedAt())
                        .build())
                .toList();

        return PlayerSessionsResponse.builder()
                .playerId(player.getId())
                .playerName(player.getName())
                .sessions(summaries)
                .build();
    }

}
