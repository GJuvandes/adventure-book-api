package com.adventurebook.api.dto;

import com.adventurebook.api.model.ConsequenceType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class GameHistoryResponse {

    private Long sessionId;
    private String playerName;
    private String bookTitle;
    private int finalHealth;
    private int maxHealth;
    private boolean alive;
    private boolean finished;
    private boolean paused;
    private int totalTurns;
    private List<TurnDto> turns;

    @Getter
    @Builder
    public static class TurnDto {
        private int turn;
        private int fromSectionId;
        private int toSectionId;
        private String choice;
        private ConsequenceType consequenceType;
        private Integer healthChange;
        private String consequenceText;
        private int healthBefore;
        private int healthAfter;
        private boolean playerDied;
        private Instant timestamp;
    }

}
