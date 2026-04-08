package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditQuestionRequestDTO;
import com.qually.qually.dto.response.AuditQuestionResponseDTO;
import com.qually.qually.groups.OnIndividualSave;
import com.qually.qually.services.AuditQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class AuditQuestionController {

    private final AuditQuestionService auditQuestionService;

    public AuditQuestionController(AuditQuestionService auditQuestionService) {
        this.auditQuestionService = auditQuestionService;
    }

    @PostMapping
    public ResponseEntity<AuditQuestionResponseDTO> createQuestion(@Validated(OnIndividualSave.class) @RequestBody AuditQuestionRequestDTO dto) {
        return ResponseEntity.ok(auditQuestionService.createQuestion(dto));
    }

    @GetMapping
    public ResponseEntity<List<AuditQuestionResponseDTO>> getAllQuestions(
            @RequestParam(required = false) Integer protocolId) {
        return ResponseEntity.ok(auditQuestionService.getAllQuestions(protocolId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditQuestionResponseDTO> getQuestionById(@PathVariable Integer id) {
        return ResponseEntity.ok(auditQuestionService.getQuestionById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditQuestionResponseDTO> updateQuestion(@PathVariable Integer id,
                                                                   @Valid @RequestBody AuditQuestionRequestDTO dto) {
        return ResponseEntity.ok(auditQuestionService.updateQuestion(id, dto));
    }
}