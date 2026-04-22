package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditSessionRequestDTO;
import com.qually.qually.dto.request.AuditSessionUpdateRequestDTO;
import com.qually.qually.dto.response.AuditSessionResponseDTO;
import com.qually.qually.services.AuditSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for {@link com.qually.qually.models.AuditSession} resources.
 *
 * <p>{@code auditorEmail} query param replaced by {@code auditorUserId}
 * (Integer) to match the new FK structure on {@code audit_sessions}.</p>
 */
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
        return ResponseEntity.ok(auditSessionService.getAllSessions(auditorUserId, auditStatus));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditSessionResponseDTO> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(auditSessionService.getSessionById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditSessionResponseDTO> updateSession(
            @PathVariable Long id,
            @RequestBody AuditSessionUpdateRequestDTO dto) {
        return ResponseEntity.ok(auditSessionService.updateSession(id, dto));
    }
}
