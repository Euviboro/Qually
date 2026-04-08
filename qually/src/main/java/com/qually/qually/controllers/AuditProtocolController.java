package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditProtocolRequestDTO;
import com.qually.qually.dto.response.AuditProtocolResponseDTO;
import com.qually.qually.groups.OnDeepSave;
import com.qually.qually.groups.OnIndividualSave;
import com.qually.qually.services.AuditProtocolService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/protocols")
public class AuditProtocolController {

    private final AuditProtocolService auditProtocolService;

    public AuditProtocolController(AuditProtocolService auditProtocolService) {
        this.auditProtocolService = auditProtocolService;
    }

    @PostMapping
    public ResponseEntity<AuditProtocolResponseDTO> createProtocol(@Validated(OnIndividualSave.class) @RequestBody AuditProtocolRequestDTO dto) {
        return ResponseEntity.ok(auditProtocolService.createProtocol(dto));
    }

    @GetMapping
    public ResponseEntity<List<AuditProtocolResponseDTO>> getAllProtocols(
            @RequestParam(required = false) Integer clientId) {
        return ResponseEntity.ok(auditProtocolService.getAllProtocols(clientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditProtocolResponseDTO> getProtocolById(@PathVariable Integer id) {
        return ResponseEntity.ok(auditProtocolService.getProtocolById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditProtocolResponseDTO> updateProtocol(@PathVariable Integer id,
                                                                   @Valid @RequestBody AuditProtocolRequestDTO dto) {
        return ResponseEntity.ok(auditProtocolService.updateProtocol(id, dto));
    }

    @PatchMapping("/{id}/finalize")
    public ResponseEntity<AuditProtocolResponseDTO> finalizeProtocol(@PathVariable Integer id) {
        return ResponseEntity.ok(auditProtocolService.finalizeProtocol(id));
    }

    @PostMapping("/deep-save")
    public ResponseEntity<AuditProtocolResponseDTO> createFullProtocol(@Validated(OnDeepSave.class) @RequestBody AuditProtocolRequestDTO dto) {
        return ResponseEntity.ok(auditProtocolService.createFullProtocol(dto));
    }
}