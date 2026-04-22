package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuditQuestionRequestDTO;
import com.qually.qually.dto.response.AuditQuestionResponseDTO;
import com.qually.qually.groups.OnIndividualSave;
import com.qually.qually.services.AuditQuestionService;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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

    /**
     * Creates a new question.
     *
     * <p><strong>Fix:</strong> was {@code @Validated(OnIndividualSave.class)}, which
     * activated only the {@code OnIndividualSave} validation group and silently skipped
     * the Default group. {@code questionText} is annotated {@code @NotBlank} with no
     * explicit group, meaning it belongs to the Default group — so it was never
     * validated on create, allowing blank question text to reach the database.</p>
     *
     * <p>Using {@code @Validated({Default.class, OnIndividualSave.class})} activates
     * both groups: Default validates {@code questionText}, OnIndividualSave validates
     * {@code category} and {@code protocolId}.</p>
     */
    @PostMapping
    public ResponseEntity<AuditQuestionResponseDTO> createQuestion(
            @Validated({Default.class, OnIndividualSave.class}) @RequestBody AuditQuestionRequestDTO dto) {
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
    public ResponseEntity<AuditQuestionResponseDTO> updateQuestion(
            @PathVariable Integer id,
            @Valid @RequestBody AuditQuestionRequestDTO dto) {
        return ResponseEntity.ok(auditQuestionService.updateQuestion(id, dto));
    }
}