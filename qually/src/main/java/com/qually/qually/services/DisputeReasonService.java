package com.qually.qually.services;

import com.qually.qually.dto.response.DisputeReasonResponseDTO;
import com.qually.qually.models.DisputeReason;
import com.qually.qually.repositories.DisputeReasonRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for {@link DisputeReason} — read-only reference data.
 *
 * <p>Dispute reasons are seeded via SQL. This service provides the API-facing
 * read methods and DTO conversion that previously happened directly in
 * {@code DisputeReasonController}.</p>
 *
 * <p>No logging added — read-only reference data with no state changes.</p>
 */
@Service
public class DisputeReasonService {

    private final DisputeReasonRepository disputeReasonRepository;

    public DisputeReasonService(DisputeReasonRepository disputeReasonRepository) {
        this.disputeReasonRepository = disputeReasonRepository;
    }

    @Transactional(readOnly = true)
    public List<DisputeReasonResponseDTO> getAllReasons() {
        return disputeReasonRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DisputeReasonResponseDTO getReasonById(Integer id) {
        return disputeReasonRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Dispute reason with ID %d not found".formatted(id)));
    }

    // ── Helpers ───────────────────────────────────────────────

    private DisputeReasonResponseDTO toDTO(DisputeReason reason) {
        return DisputeReasonResponseDTO.builder()
                .reasonId(reason.getReasonId())
                .reasonText(reason.getReasonText())
                .build();
    }
}