package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditDisputeRequestDTO;
import com.qually.qually.dto.request.ResolveDisputeRequestDTO;
import com.qually.qually.dto.response.AuditDisputeResponseDTO;
import com.qually.qually.dto.response.DisputeInboxRowDTO;
import com.qually.qually.services.DisputeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<DisputeInboxRowDTO>> getInbox() {
        return ResponseEntity.ok(disputeService.getInbox(currentUserId()));
    }

    @PostMapping("/flag/{responseId}")
    public ResponseEntity<Void> flagResponse(@PathVariable Long responseId) {
        disputeService.flagResponse(responseId, currentUserId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/flag/{responseId}")
    public ResponseEntity<Void> unflagResponse(@PathVariable Long responseId) {
        disputeService.unflagResponse(responseId, currentUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/raise")
    public ResponseEntity<AuditDisputeResponseDTO> raiseDispute(
            @Valid @RequestBody AuditDisputeRequestDTO dto) {
        return ResponseEntity.ok(disputeService.raiseDispute(dto, currentUserId()));
    }

    @PutMapping("/resolve/{disputeId}")
    public ResponseEntity<AuditDisputeResponseDTO> resolveDispute(
            @PathVariable Integer disputeId,
            @Valid @RequestBody ResolveDisputeRequestDTO dto) {
        return ResponseEntity.ok(disputeService.resolveDispute(disputeId, dto, currentUserId()));
    }

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}