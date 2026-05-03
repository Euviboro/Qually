package com.qually.qually.controllers;

import com.qually.qually.dto.request.SubattributeResponseRequestDTO;
import com.qually.qually.dto.response.AttributeAnswerResponseDTO;
import com.qually.qually.services.AttributeResponseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only REST controller for managing subattribute responses individually.
 *
 * <p><strong>Important:</strong> the primary write path for subattribute responses
 * goes through {@link AuditResponseController} via
 * {@code POST /api/audit-responses/bulk}, which saves all responses and their
 * subattribute selections in a single transaction as part of the session log flow.
 * That path should always be preferred during normal operation.</p>
 *
 * <p>This controller exists for administrative use only — for example, inspecting
 * or correcting individual subattribute selections outside the normal audit flow.
 * It should not be called by the frontend under normal circumstances.</p>
 */

@RestController
@RequestMapping("/api/attribute-responses")
public class AttributeResponseController {

    private final AttributeResponseService attributeResponseService;

    public AttributeResponseController(AttributeResponseService attributeResponseService) {
        this.attributeResponseService = attributeResponseService;
    }

    @PostMapping
    public ResponseEntity<AttributeAnswerResponseDTO> createAttributeResponse(
            @Valid @RequestBody SubattributeResponseRequestDTO dto) {
        return ResponseEntity.ok(attributeResponseService.createAttributeResponse(dto));
    }

    @GetMapping
    public ResponseEntity<List<AttributeAnswerResponseDTO>> getAttributeResponsesByAuditResponse(
            @RequestParam Long auditResponseId) {
        return ResponseEntity.ok(attributeResponseService.getAttributeResponsesByAuditResponse(auditResponseId));
    }
}