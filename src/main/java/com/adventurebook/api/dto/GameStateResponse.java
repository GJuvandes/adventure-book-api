package com.adventurebook.api.dto;

import com.adventurebook.api.model.ConsequenceType;
import com.adventurebook.api.model.SectionType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GameStateResponse {

    private Long sessionId;
    private String playerName;
    private String bookTitle;
    private int health;
    private int maxHealth;
    private boolean alive;
    private boolean finished;
    private boolean paused;
    private int currentSectionId;
    private SectionType sectionType;
    private String text;
    private ConsequenceApplied consequenceApplied;
    private List<OptionDto> options;

    @Getter
    @Builder
    public static class OptionDto {
        private int index;
        private String description;
        private boolean hasConsequence;
    }

    @Getter
    @Builder
    public static class ConsequenceApplied {
        private ConsequenceType type;
        private int healthChange;
        private int healthBefore;
        private int healthAfter;
        private String message;
    }

}
