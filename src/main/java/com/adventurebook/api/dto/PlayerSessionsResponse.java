package com.adventurebook.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class PlayerSessionsResponse {

    private Long playerId;
    private String playerName;
    private List<SessionSummary> sessions;

    @Getter
    @Builder
    public static class SessionSummary {
        private Long sessionId;
        private Long bookId;
        private String bookTitle;
        private int health;
        private int maxHealth;
        private boolean alive;
        private boolean finished;
        private boolean paused;
        private int currentSectionId;
        private int totalTurns;
        private Instant lastPlayedAt;
    }

}
