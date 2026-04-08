package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditedProcessRequestDTO;
import com.qually.qually.dto.response.AuditedProcessResponseDTO;
import com.qually.qually.services.AuditedProcessService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/processes")
public class AuditedProcessController {

    private final AuditedProcessService auditedProcessService;

    public AuditedProcessController(AuditedProcessService auditedProcessService) {
        this.auditedProcessService = auditedProcessService;
    }

    @PostMapping
    public ResponseEntity<AuditedProcessResponseDTO> createProcess(@Valid @RequestBody AuditedProcessRequestDTO dto) {
        return ResponseEntity.ok(auditedProcessService.createProcess(dto));
    }

    @GetMapping
    public ResponseEntity<List<AuditedProcessResponseDTO>> getAllProcesses() {
        return ResponseEntity.ok(auditedProcessService.getAllProcesses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditedProcessResponseDTO> getProcessById(@PathVariable Integer id) {
        return ResponseEntity.ok(auditedProcessService.getProcessById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditedProcessResponseDTO> updateProcess(@PathVariable Integer id,
                                                                   @Valid @RequestBody AuditedProcessRequestDTO dto) {
        return ResponseEntity.ok(auditedProcessService.updateProcess(id, dto));
    }
}