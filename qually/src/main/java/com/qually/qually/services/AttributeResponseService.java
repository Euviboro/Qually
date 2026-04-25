package com.qually.qually.services;

import com.qually.qually.dto.request.SubattributeResponseRequestDTO;
import com.qually.qually.dto.response.AttributeAnswerResponseDTO;
import com.qually.qually.models.AuditResponse;
import com.qually.qually.models.SubattributeOption;
import com.qually.qually.models.SubattributeResponse;
import com.qually.qually.repositories.AuditResponseRepository;
import com.qually.qually.repositories.SubattributeOptionRepository;
import com.qually.qually.repositories.SubattributeResponseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for the standalone subattribute response endpoints.
 *
 * <p>Rewritten to match the updated {@link SubattributeResponse} entity which
 * links to {@link SubattributeOption} (via {@code selectedOption}) rather than
 * to {@code Subattribute} with a free-text {@code answerValue}. The previous
 * version referenced fields ({@code subattribute}, {@code answerValue},
 * {@code attributeResponseId}) that no longer exist on the entity.</p>
 *
 * <p>Note: subattribute responses are normally created through
 * {@link AuditResponseService#saveBulkResponses} as part of session submission.
 * This service and its controller ({@code /api/attribute-responses}) exist for
 * standalone access and query use cases.</p>
 */
@Service
public class AttributeResponseService {

    private final SubattributeResponseRepository subattributeResponseRepository;
    private final AuditResponseRepository auditResponseRepository;
    private final SubattributeOptionRepository subattributeOptionRepository;

    public AttributeResponseService(
            SubattributeResponseRepository subattributeResponseRepository,
            AuditResponseRepository auditResponseRepository,
            SubattributeOptionRepository subattributeOptionRepository) {
        this.subattributeResponseRepository = subattributeResponseRepository;
        this.auditResponseRepository = auditResponseRepository;
        this.subattributeOptionRepository = subattributeOptionRepository;
    }

    /**
     * Creates a standalone subattribute response linking an audit response to
     * the option the auditor selected.
     *
     * @param dto Contains {@code auditResponseId} and {@code subattributeOptionId}.
     * @return The persisted subattribute response as a DTO.
     */
    @Transactional
    public AttributeAnswerResponseDTO createAttributeResponse(
            SubattributeResponseRequestDTO dto) {
        AuditResponse auditResponse = auditResponseRepository
                .findById(dto.getAuditResponseId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Audit response with ID %d not found"
                        .formatted(dto.getAuditResponseId())));

        SubattributeOption option = subattributeOptionRepository
                .findById(dto.getSubattributeOptionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subattribute option with ID %d not found"
                        .formatted(dto.getSubattributeOptionId())));

        SubattributeResponse saved = subattributeResponseRepository.save(
                SubattributeResponse.builder()
                        .auditResponse(auditResponse)
                        .selectedOption(option)
                        .build());

        return toDTO(saved);
    }

    /**
     * Returns all subattribute responses for a given audit response.
     *
     * @param auditResponseId The audit response to query.
     */
    @Transactional(readOnly = true)
    public List<AttributeAnswerResponseDTO> getAttributeResponsesByAuditResponse(
            Long auditResponseId) {
        return subattributeResponseRepository
                .findByAuditResponse_AuditResponseId(auditResponseId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────

    private AttributeAnswerResponseDTO toDTO(SubattributeResponse sr) {
        SubattributeOption option = sr.getSelectedOption();
        return AttributeAnswerResponseDTO.builder()
                .subattributeResponseId(sr.getSubattributeResponseId())
                .auditResponseId(sr.getAuditResponse().getAuditResponseId())
                .subattributeId(option.getSubattribute().getSubattributeId())
                .subattributeText(option.getSubattribute().getSubattributeText())
                .optionId(option.getSubattributeOptionId())
                .optionLabel(option.getOptionLabel())
                .build();
    }
}
