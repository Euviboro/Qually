package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditSessionRequestDTO;
import com.qually.qually.dto.request.AuditSessionUpdateRequestDTO;
import com.qually.qually.dto.response.AuditSessionResponseDTO;
import com.qually.qually.dto.response.SessionResultsResponseDTO;
import com.qually.qually.dto.response.SessionResumeDTO;
import com.qually.qually.services.AuditSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(required = false) String auditStatus,
            HttpServletRequest req) {
        return ResponseEntity.ok(auditSessionService.getAllSessions(
                auditorUserId, auditStatus, currentUserIdOrNull(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditSessionResponseDTO> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(auditSessionService.getSessionById(id));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<SessionResultsResponseDTO> getSessionResults(@PathVariable Long id) {
        return ResponseEntity.ok(auditSessionService.getSessionResults(id));
    }

    /**
     * Returns the minimal payload needed to resume a DRAFT session in the
     * Log Session form: metadata fields + previously recorded answers with
     * subattribute selections.
     *
     * <p>Only DRAFT sessions can be resumed — returns 403 for any other status.</p>
     */
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

    private Integer currentUserIdOrNull(HttpServletRequest req) {
        String header = req.getHeader("X-User-Id");
        if (header == null || header.isBlank()) return null;
        try { return Integer.parseInt(header); } catch (NumberFormatException e) { return null; }
    }
}