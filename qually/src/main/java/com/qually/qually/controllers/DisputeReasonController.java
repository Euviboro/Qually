package com.qually.qually.controllers;

import com.qually.qually.dto.response.DisputeReasonResponseDTO;
import com.qually.qually.services.DisputeReasonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for {@link com.qually.qually.models.DisputeReason}.
 *
 * <p>Previously called {@code DisputeReasonRepository} directly — now goes
 * through {@link DisputeReasonService} following the standard
 * controller → service → repository architecture.</p>
 */
@RestController
@RequestMapping("/api/dispute-reasons")
public class DisputeReasonController {

    private final DisputeReasonService disputeReasonService;

    public DisputeReasonController(DisputeReasonService disputeReasonService) {
        this.disputeReasonService = disputeReasonService;
    }

    @GetMapping
    public ResponseEntity<List<DisputeReasonResponseDTO>> getAllReasons() {
        return ResponseEntity.ok(disputeReasonService.getAllReasons());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisputeReasonResponseDTO> getReasonById(@PathVariable Integer id) {
        return ResponseEntity.ok(disputeReasonService.getReasonById(id));
    }
}