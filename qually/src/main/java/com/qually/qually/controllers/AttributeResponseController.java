package com.qually.qually.controllers;

import com.qually.qually.dto.request.SubattributeResponseRequestDTO;
import com.qually.qually.dto.response.AttributeAnswerResponseDTO;
import com.qually.qually.services.AttributeResponseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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