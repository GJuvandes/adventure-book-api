package com.adventurebook.api.exception;

import com.adventurebook.api.model.ErrorLog;
import com.adventurebook.api.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final int MAX_STACK_TRACE_LENGTH = 6000; // To fit within VARCHAR(6000) of the database

    private final ErrorLogRepository errorLogRepository;

    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBookNotFound(BookNotFoundException ex) {
        log.warn("Book not found: {}", ex.getMessage());

        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), ex);
    }

    @ExceptionHandler(InvalidBookException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBook(InvalidBookException ex) {
        log.warn("Invalid book submitted: {} — errors: {}", ex.getMessage(), ex.getValidationErrors());

        final Map<String, Object> extras = new HashMap<>();
        extras.put("validationErrors", ex.getValidationErrors());

        if (ex.getWarnings() != null && !ex.getWarnings().isEmpty()) {
            extras.put("warnings", ex.getWarnings());
        }

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Book", ex.getMessage(), ex, extras);
    }

    @ExceptionHandler(GameSessionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSessionNotFound(GameSessionNotFoundException ex) {
        log.warn("Game session not found: {}", ex.getMessage());

        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), ex);
    }

    @ExceptionHandler(GameOverException.class)
    public ResponseEntity<Map<String, Object>> handleGameOver(GameOverException ex) {
        log.warn("Game over action attempted: {}", ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Game Over", ex.getMessage(), ex);
    }

    @ExceptionHandler(InvalidChoiceException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidChoice(InvalidChoiceException ex) {
        log.warn("Invalid choice: {}", ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Choice", ex.getMessage(), ex);
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePlayerNotFound(PlayerNotFoundException ex) {
        log.warn("Player not found: {}", ex.getMessage());

        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), ex);
    }

    @ExceptionHandler(DuplicatePlayerNameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePlayerName(DuplicatePlayerNameException ex) {
        log.warn("Duplicate player name: {}", ex.getMessage());

        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), ex);
    }

    @ExceptionHandler(GamePausedException.class)
    public ResponseEntity<Map<String, Object>> handleGamePaused(GamePausedException ex) {
        log.warn("Action on paused game: {}", ex.getMessage());

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Game Paused", ex.getMessage(), ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        final String message = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();

        log.warn("Type mismatch: {}", message);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", message, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        final List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();

        final String message = "Validation failed";
        log.warn("Validation error: {}", fieldErrors);

        final Map<String, Object> extras = Map.of("validationErrors", fieldErrors);

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation Error", message, ex, extras);
    }

    /**
     * For the favicon.co request that the browser makes for which we don't have a handler for,
     * without this it logs an error stack trace which is noisy and not actionable
    */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred.", ex);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
        HttpStatus status, String error,
        String message, Exception ex
    ) {
        return buildErrorResponse(status, error, message, ex, Map.of());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
        HttpStatus status, String error,
        String message, Exception ex,
        Map<String, Object> extras
    ) {
        try {
            final StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();

            if (stackTrace.length() > MAX_STACK_TRACE_LENGTH) {
                stackTrace = stackTrace.substring(0, MAX_STACK_TRACE_LENGTH);
            }

            final ErrorLog errorLog = ErrorLog.builder()
                                        .timestamp(Instant.now())
                                        .status(status.value())
                                        .error(error)
                                        .message(ex.getMessage() != null ? ex.getMessage() : "No message")
                                        .exceptionClass(ex.getClass().getSimpleName())
                                        .stackTrace(stackTrace)
                                        .build();

            errorLogRepository.save(errorLog);
        } catch (Exception e) {
            log.error("Failed to save error log: {}", e.getMessage(), e);
        }

        final Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.putAll(extras);

        return ResponseEntity.status(status).body(body);
    }
}
