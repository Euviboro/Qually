package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditDisputeRequestDTO;
import com.qually.qually.dto.request.ResolveDisputeRequestDTO;
import com.qually.qually.dto.response.AuditDisputeResponseDTO;
import com.qually.qually.services.DisputeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the dispute lifecycle.
 *
 * <p>All endpoints read the acting user's ID from the {@code X-User-Id} header,
 * which the frontend includes on every request via the mock auth context. Each
 * service method performs its own permission checks against that user ID.</p>
 */
@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /** Flag a response as needing review. No dispute record is created yet. */
    @PostMapping("/flag/{responseId}")
    public ResponseEntity<Void> flagResponse(@PathVariable Long responseId,
                                              HttpServletRequest req) {
        disputeService.flagResponse(responseId, currentUserId(req));
        return ResponseEntity.ok().build();
    }

    /** Remove a flag from a response — returns it to ANSWERED with no paper trail. */
    @DeleteMapping("/flag/{responseId}")
    public ResponseEntity<Void> unflagResponse(@PathVariable Long responseId,
                                                HttpServletRequest req) {
        disputeService.unflagResponse(responseId, currentUserId(req));
        return ResponseEntity.ok().build();
    }

    /** Formally raise a dispute. Creates an audit_disputes entry and updates session status. */
    @PostMapping("/raise")
    public ResponseEntity<AuditDisputeResponseDTO> raiseDispute(
            @Valid @RequestBody AuditDisputeRequestDTO dto,
            HttpServletRequest req) {
        return ResponseEntity.ok(disputeService.raiseDispute(dto, currentUserId(req)));
    }

    /** Resolve a dispute. Triggers score recalculation if outcome is MODIFIED. */
    @PutMapping("/resolve/{disputeId}")
    public ResponseEntity<AuditDisputeResponseDTO> resolveDispute(
            @PathVariable Integer disputeId,
            @Valid @RequestBody ResolveDisputeRequestDTO dto,
            HttpServletRequest req) {
        return ResponseEntity.ok(disputeService.resolveDispute(disputeId, dto, currentUserId(req)));
    }

    // ── Helpers ───────────────────────────────────────────────

    private Integer currentUserId(HttpServletRequest req) {
        String header = req.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new IllegalStateException("X-User-Id header is required");
        }
        return Integer.parseInt(header);
    }
}
