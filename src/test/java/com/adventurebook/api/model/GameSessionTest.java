package com.adventurebook.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {

    @Test
    void applyHealthChange_losesHealth() {
        GameSession session = GameSession.builder()
                                         .health(10)
                                         .alive(true)
                                         .build();

        session.applyHealthChange(-3);

        assertEquals(7, session.getHealth());
        assertTrue(session.isAlive());
    }

    @Test
    void applyHealthChange_gainsHealth() {
        GameSession session = GameSession.builder()
                                         .health(5)
                                         .alive(true)
                                         .build();

        session.applyHealthChange(3);

        assertEquals(8, session.getHealth());
    }

    @Test
    void applyHealthChange_clampedAtMax() {
        GameSession session = GameSession.builder()
                                         .health(8)
                                         .alive(true)
                                         .build();

        session.applyHealthChange(10);

        assertEquals(GameSession.MAX_HEALTH, session.getHealth());
    }

    @Test
    void applyHealthChange_playerDies_clampedAtZero() {
        GameSession session = GameSession.builder()
                                         .health(3)
                                         .alive(true)
                                         .finished(false)
                                         .build();

        session.applyHealthChange(-5);

        assertEquals(0, session.getHealth());
        assertFalse(session.isAlive());
        assertTrue(session.isFinished());
        assertTrue(session.isDead());
    }
}
