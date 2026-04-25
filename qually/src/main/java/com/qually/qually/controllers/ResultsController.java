package com.qually.qually.controllers;

import com.qually.qually.dto.response.PagedResultsResponseDTO;
import com.qually.qually.services.ResultsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Provides the paginated flat row data for the Results table.
 *
 * <p>All requests require an {@code X-User-Id} header so the service can apply
 * the correct visibility tier for the current user.</p>
 *
 * <p>The response is now wrapped in {@link PagedResultsResponseDTO} which
 * includes {@code totalElements}, {@code totalPages}, and {@code currentPage}
 * so the frontend can render a numbered pagination control.</p>
 */
@RestController
@RequestMapping("/api/results")
public class ResultsController {

    private final ResultsService resultsService;

    public ResultsController(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    /**
     * @param protocolId     Optional — limits rows to one protocol.
     * @param clientId       Optional — limits rows to one client.
     * @param auditorId      Optional — limits rows to sessions by one auditor.
     * @param memberId       Optional — limits rows to sessions for one member.
     * @param includeAnswers When true, populates question answer columns.
     * @param page           Zero-based page index (default 0).
     * @param size           Rows per page (default {@value ResultsService#DEFAULT_PAGE_SIZE},
     *                       max {@value ResultsService#MAX_PAGE_SIZE}).
     */
    @GetMapping
    public ResponseEntity<PagedResultsResponseDTO> getResults(
            @RequestParam(required = false) Integer protocolId,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) Integer auditorId,
            @RequestParam(required = false) Integer memberId,
            @RequestParam(required = false, defaultValue = "false") boolean includeAnswers,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "100") int size,
            HttpServletRequest req) {

        return ResponseEntity.ok(resultsService.getResults(
                currentUserId(req),
                protocolId, clientId, auditorId, memberId,
                includeAnswers, page, size));
    }

    private Integer currentUserId(HttpServletRequest req) {
        String header = req.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new IllegalStateException("X-User-Id header is required");
        }
        return Integer.parseInt(header); // NumberFormatException handled by GlobalExceptionHandler
    }
}
