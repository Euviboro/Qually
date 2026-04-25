package com.qually.qually.controllers;

import com.qually.qually.dto.request.BulkAuditAnswerRequestDTO;
import com.qually.qually.dto.response.AuditResponseDTO;
import com.qually.qually.services.AuditResponseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responses")
public class AuditResponseController {

    private final AuditResponseService auditResponseService;

    public AuditResponseController(AuditResponseService auditResponseService) {
        this.auditResponseService = auditResponseService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<AuditResponseDTO>> submitBulkAnswers(
            @Valid @RequestBody BulkAuditAnswerRequestDTO dto) {
        return ResponseEntity.ok(auditResponseService.saveBulkResponses(dto));
    }

    @GetMapping
    public ResponseEntity<List<AuditResponseDTO>> getResponsesBySession(
            @RequestParam Long sessionId) {
        return ResponseEntity.ok(auditResponseService.getResponsesBySession(sessionId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditResponseDTO> getResponseById(@PathVariable Long id) {
        return ResponseEntity.ok(auditResponseService.getResponseById(id));
    }
}