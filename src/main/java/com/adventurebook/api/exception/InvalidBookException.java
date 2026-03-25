package com.adventurebook.api.exception;

import java.util.List;

public class InvalidBookException extends RuntimeException {

    private final List<String> validationErrors;
    private final List<String> warnings;

    public InvalidBookException(List<String> validationErrors, List<String> warnings) {
        super("Book validation failed: " + String.join("; ", validationErrors));
        this.validationErrors = validationErrors;
        this.warnings = warnings;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

}
