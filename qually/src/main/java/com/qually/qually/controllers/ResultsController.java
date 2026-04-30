package com.qually.qually.controllers;

import com.qually.qually.dto.response.PagedResultsResponseDTO;
import com.qually.qually.services.ResultsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Provides the paginated flat row data for the Results table.
 * User identity is read from the JWT security context — no header required.
 */
@RestController
@RequestMapping("/api/results")
public class ResultsController {

    private final ResultsService resultsService;

    public ResultsController(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    @GetMapping
    public ResponseEntity<PagedResultsResponseDTO> getResults(
            @RequestParam(required = false) Integer protocolId,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer auditorId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false, defaultValue = "false") boolean includeAnswers,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int size) {

        return ResponseEntity.ok(resultsService.getResults(
                currentUserId(),
                protocolId, clientId, auditorId, memberId,
                includeAnswers, page, size));
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}