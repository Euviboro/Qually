package com.qually.qually.controllers;

import com.qually.qually.dto.request.SubattributeRequestDTO;
import com.qually.qually.dto.response.SubattributeResponseDTO;
import com.qually.qually.services.SubattributeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attributes")
public class AttributeController {

    private final SubattributeService subattributeService;

    public AttributeController(SubattributeService subattributeService) {
        this.subattributeService = subattributeService;
    }

    @PostMapping
    public ResponseEntity<SubattributeResponseDTO> createAttribute(@Valid @RequestBody SubattributeRequestDTO dto) {
        return ResponseEntity.ok(subattributeService.createSubattribute(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubattributeResponseDTO> getAttributeById(@PathVariable Integer id) {
        return ResponseEntity.ok(subattributeService.getAttributeById(id));
    }

    @GetMapping
    public ResponseEntity<List<SubattributeResponseDTO>> getAttributesByQuestionId(
            @RequestParam Integer questionId) {
        return ResponseEntity.ok(subattributeService.getAttributesByQuestionId(questionId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubattributeResponseDTO> updateAttribute(@PathVariable Integer id,
                                                                   @Valid @RequestBody SubattributeRequestDTO dto) {
        return ResponseEntity.ok(subattributeService.updateAttribute(id, dto));
    }
}