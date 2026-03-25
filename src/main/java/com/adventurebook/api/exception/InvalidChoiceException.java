package com.adventurebook.api.exception;

public class InvalidChoiceException extends RuntimeException {

    public InvalidChoiceException(int optionIndex, int totalOptions) {
        super("Invalid choice: " + optionIndex + ". Valid options are 0 to " + (totalOptions - 1) + ".");
    }

}
