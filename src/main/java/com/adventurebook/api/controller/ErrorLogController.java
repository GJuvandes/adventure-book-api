package com.adventurebook.api.controller;

import com.adventurebook.api.model.ErrorLog;
import com.adventurebook.api.repository.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/errors")
@RequiredArgsConstructor
public class ErrorLogController {

    private final ErrorLogRepository errorLogRepository;

    @GetMapping
    public ResponseEntity<List<ErrorLog>> getErrors(
            @RequestParam(required = false, defaultValue = "24") int hours
    ) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        return ResponseEntity.ok(errorLogRepository.findByTimestampAfterOrderByTimestampDesc(since));
    }

}
