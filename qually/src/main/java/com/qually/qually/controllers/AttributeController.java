package com.qually.qually.controllers;

import com.qually.qually.dto.request.SubattributeRequestDTO;
import com.qually.qually.dto.response.SubattributeResponseDTO;
import com.qually.qually.services.AttributeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attributes")
public class AttributeController {

    private final AttributeService attributeService;

    public AttributeController(AttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @PostMapping
    public ResponseEntity<SubattributeResponseDTO> createAttribute(@Valid @RequestBody SubattributeRequestDTO dto) {
        return ResponseEntity.ok(attributeService.createAttribute(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubattributeResponseDTO> getAttributeById(@PathVariable Integer id) {
        return ResponseEntity.ok(attributeService.getAttributeById(id));
    }

    @GetMapping
    public ResponseEntity<List<SubattributeResponseDTO>> getAttributesByQuestionId(
            @RequestParam Integer questionId) {
        return ResponseEntity.ok(attributeService.getAttributesByQuestionId(questionId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubattributeResponseDTO> updateAttribute(@PathVariable Integer id,
                                                                   @Valid @RequestBody SubattributeRequestDTO dto) {
        return ResponseEntity.ok(attributeService.updateAttribute(id, dto));
    }
}