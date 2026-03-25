package com.adventurebook.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSession {

    public static final int MAX_HEALTH = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    @JsonIgnore
    private Player player;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    @JsonIgnore
    private Book book;

    @Column(nullable = false)
    private int currentSectionId;

    @Column(nullable = false)
    private int health;

    @Column(nullable = false)
    private boolean alive;

    @Column(nullable = false)
    private boolean finished;

    @Column(nullable = false)
    @Builder.Default
    private boolean paused = false;

    @Column(nullable = false)
    @Builder.Default
    private Instant lastPlayedAt = Instant.now();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("turnNumber ASC")
    @Builder.Default
    private List<GameEvent> events = new ArrayList<>();

    public boolean isDead() {
        return !alive;
    }

    public void applyHealthChange(final int healthChange) {
        this.health = Math.max(0, Math.min(this.health + healthChange, MAX_HEALTH));
        if (this.health <= 0) {
            this.alive = false;
            this.finished = true;
        }
    }

}
