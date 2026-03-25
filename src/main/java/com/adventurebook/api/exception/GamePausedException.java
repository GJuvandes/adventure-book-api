package com.adventurebook.api.exception;

public class GamePausedException extends RuntimeException {

    public GamePausedException(Long sessionId) {
        super("Game session " + sessionId + " is paused. Resume it before making choices.");
    }

}
