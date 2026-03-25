package com.adventurebook.api.exception;

public class GameOverException extends RuntimeException {

    public GameOverException(String reason) {
        super("Game is over: " + reason);
    }

}
