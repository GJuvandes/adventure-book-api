package com.adventurebook.api.exception;

public class GameSessionNotFoundException extends RuntimeException {

    public GameSessionNotFoundException(Long id) {
        super("Game session not found with id: " + id);
    }

}
