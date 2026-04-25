package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditDisputeResponseDTO {
    private Integer disputeId;
    private Long responseId;
    private Integer raisedByUserId;
    private String raisedByName;
    private Integer reasonId;
    private String reasonText;
    private String disputeComment;
    private LocalDateTime raisedAt;
    private Integer resolvedByUserId;
    private String resolvedByName;
    private LocalDateTime resolutionDate;
    private String resolutionNote;
    private String resolutionOutcome;
    private String newAnswer;
}
