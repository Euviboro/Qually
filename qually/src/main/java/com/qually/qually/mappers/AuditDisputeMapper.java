package com.qually.qually.mappers;

import com.qually.qually.dto.response.AuditDisputeResponseDTO;
import com.qually.qually.models.AuditDispute;
import com.qually.qually.models.User;
import org.springframework.stereotype.Component;

@Component
public class AuditDisputeMapper {

    /**
     * Maps a persisted {@link AuditDispute} to its response DTO.
     * Returns {@code null} when the dispute is {@code null} — callers can use
     * this as a null-safe check before including the dispute in a result DTO.
     */
    public AuditDisputeResponseDTO toDTO(AuditDispute dispute) {
        if (dispute == null) return null;

        User raisedBy  = dispute.getRaisedBy();
        User resolvedBy = dispute.getResolvedBy();

        return AuditDisputeResponseDTO.builder()
                .disputeId(dispute.getDisputeId())
                .responseId(dispute.getResponse().getAuditResponseId())
                .raisedByUserId(raisedBy != null ? raisedBy.getUserId() : null)
                .raisedByName(raisedBy != null ? raisedBy.getFullName() : null)
                .reasonId(dispute.getReason().getReasonId())
                .reasonText(dispute.getReason().getReasonText())
                .disputeComment(dispute.getDisputeComment())
                .raisedAt(dispute.getRaisedAt())
                .resolvedByUserId(resolvedBy != null ? resolvedBy.getUserId() : null)
                .resolvedByName(resolvedBy != null ? resolvedBy.getFullName() : null)
                .resolutionDate(dispute.getResolutionDate())
                .resolutionNote(dispute.getResolutionNote())
                .resolutionOutcome(dispute.getResolutionOutcome() != null
                        ? dispute.getResolutionOutcome().name() : null)
                .newAnswer(dispute.getNewAnswer())
                .build();
    }
}
