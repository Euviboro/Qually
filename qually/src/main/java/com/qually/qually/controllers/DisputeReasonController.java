package com.qually.qually.controllers;

import com.qually.qually.models.DisputeReason;
import com.qually.qually.repositories.DisputeReasonRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exposes the predefined dispute reason list for the frontend SearchableSelect. */
@RestController
@RequestMapping("/api/dispute-reasons")
public class DisputeReasonController {

    private final DisputeReasonRepository disputeReasonRepository;

    public DisputeReasonController(DisputeReasonRepository disputeReasonRepository) {
        this.disputeReasonRepository = disputeReasonRepository;
    }

    @GetMapping
    public ResponseEntity<List<DisputeReason>> getAll() {
        return ResponseEntity.ok(disputeReasonRepository.findAll());
    }
}
