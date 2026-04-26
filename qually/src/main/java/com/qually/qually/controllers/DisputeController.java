package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditDisputeRequestDTO;
import com.qually.qually.dto.request.ResolveDisputeRequestDTO;
import com.qually.qually.dto.response.AuditDisputeResponseDTO;
import com.qually.qually.dto.response.DisputeInboxRowDTO;
import com.qually.qually.services.DisputeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * Returns the disputes inbox scoped to the current user's role and
     * visibility tier. See {@link DisputeService#getInbox} for tier logic.
     */
    @GetMapping("/inbox")
    public ResponseEntity<List<DisputeInboxRowDTO>> getInbox(HttpServletRequest req) {
        return ResponseEntity.ok(disputeService.getInbox(currentUserId(req)));
    }

    @PostMapping("/flag/{responseId}")
    public ResponseEntity<Void> flagResponse(@PathVariable Long responseId,
                                             HttpServletRequest req) {
        disputeService.flagResponse(responseId, currentUserId(req));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/flag/{responseId}")
    public ResponseEntity<Void> unflagResponse(@PathVariable Long responseId,
                                               HttpServletRequest req) {
        disputeService.unflagResponse(responseId, currentUserId(req));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/raise")
    public ResponseEntity<AuditDisputeResponseDTO> raiseDispute(
            @Valid @RequestBody AuditDisputeRequestDTO dto,
            HttpServletRequest req) {
        return ResponseEntity.ok(disputeService.raiseDispute(dto, currentUserId(req)));
    }

    @PutMapping("/resolve/{disputeId}")
    public ResponseEntity<AuditDisputeResponseDTO> resolveDispute(
            @PathVariable Integer disputeId,
            @Valid @RequestBody ResolveDisputeRequestDTO dto,
            HttpServletRequest req) {
        return ResponseEntity.ok(disputeService.resolveDispute(disputeId, dto, currentUserId(req)));
    }

    private Integer currentUserId(HttpServletRequest req) {
        String header = req.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new IllegalStateException("X-User-Id header is required");
        }
        return Integer.parseInt(header);
    }
}