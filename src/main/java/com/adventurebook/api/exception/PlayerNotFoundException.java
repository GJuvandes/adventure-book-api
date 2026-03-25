package com.adventurebook.api.exception;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(Long id) {
        super("Player not found with id: " + id);
    }

}
