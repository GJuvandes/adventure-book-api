package com.adventurebook.api.exception;

public class DuplicatePlayerNameException extends RuntimeException {

    public DuplicatePlayerNameException(String name) {
        super("A player with the name '" + name + "' already exists.");
    }

}
