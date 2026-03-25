package com.adventurebook.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "game_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private GameSession session;

    @Column(nullable = false)
    private int turnNumber;

    @Column(nullable = false)
    private int fromSectionId;

    @Column(nullable = false)
    private int toSectionId;

    private String choiceDescription;

    @Enumerated(EnumType.STRING)
    private ConsequenceType consequenceType;

    private Integer healthChange;

    private String consequenceText;

    @Column(nullable = false)
    private int healthBefore;

    @Column(nullable = false)
    private int healthAfter;

    @Column(nullable = false)
    private boolean playerDied;

    @Builder.Default
    @Column(nullable = false)
    private Instant timestamp = Instant.now();

}
