package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditProtocolRequestDTO;
import com.qually.qually.dto.response.AuditProtocolResponseDTO;
import com.qually.qually.groups.OnDeepSave;
import com.qually.qually.groups.OnIndividualSave;
import com.qually.qually.services.AuditProtocolService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/protocols")
public class AuditProtocolController {

    private final AuditProtocolService auditProtocolService;

    public AuditProtocolController(AuditProtocolService auditProtocolService) {
        this.auditProtocolService = auditProtocolService;
    }

    @PostMapping
    public ResponseEntity<AuditProtocolResponseDTO> createProtocol(@RequestBody AuditProtocolRequestDTO dto) {
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
        return ResponseEntity.ok(auditProtocolService.createProtocol(dto));
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<AuditProtocolResponseDTO> updateProtocolName(
            @PathVariable Integer id,
            @RequestBody Map<String, String> payload) {

        String newName = payload.get("protocolName");
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        AuditProtocolResponseDTO updatedProtocol = auditProtocolService.updateProtocolName(id, newName.trim());
        return ResponseEntity.ok(updatedProtocol);
    }
}