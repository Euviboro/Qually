package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditSessionRequestDTO;
import com.qually.qually.dto.request.AuditSessionUpdateRequestDTO;
import com.qually.qually.dto.response.AuditSessionResponseDTO;
import com.qually.qually.dto.response.SessionResultsResponseDTO;
import com.qually.qually.dto.response.SessionResumeDTO;
import com.qually.qually.services.AuditSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class AuditSessionController {

    private final AuditSessionService auditSessionService;

    public AuditSessionController(AuditSessionService auditSessionService) {
        this.auditSessionService = auditSessionService;
    }

    @PostMapping
    public ResponseEntity<AuditSessionResponseDTO> createSession(
            @Valid @RequestBody AuditSessionRequestDTO dto) {
        return ResponseEntity.ok(auditSessionService.createSession(dto));
    }

    @GetMapping
    public ResponseEntity<List<AuditSessionResponseDTO>> getAllSessions(
            @RequestParam(required = false) Integer auditorUserId,
            @RequestParam(required = false) String auditStatus) {
        return ResponseEntity.ok(auditSessionService.getAllSessions(
                auditorUserId, auditStatus, currentUserIdOrNull()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditSessionResponseDTO> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(auditSessionService.getSessionById(id));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<SessionResultsResponseDTO> getSessionResults(@PathVariable Long id) {
        return ResponseEntity.ok(auditSessionService.getSessionResults(id));
    }

    @GetMapping("/{id}/resume")
    public ResponseEntity<SessionResumeDTO> getSessionForResume(@PathVariable Long id) {
        return ResponseEntity.ok(auditSessionService.getSessionForResume(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditSessionResponseDTO> updateSession(
            @PathVariable Long id,
            @RequestBody AuditSessionUpdateRequestDTO dto) {
        return ResponseEntity.ok(auditSessionService.updateSession(id, dto));
    }

    // ── Helpers ───────────────────────────────────────────────

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }

    /**
     * Returns the current user's ID or null if unauthenticated.
     * Used by {@code getAllSessions} which applies optional visibility
     * scoping for OPERATIONS users but still works when called without auth
     * in test contexts.
     */
    private Integer currentUserIdOrNull() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        return (principal instanceof Integer) ? (Integer) principal : null;
    }
}