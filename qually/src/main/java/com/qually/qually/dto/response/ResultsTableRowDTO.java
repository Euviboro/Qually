package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Flat row DTO used exclusively by the Results table.
 *
 * <p>Each row represents one audit session. Question answers are included
 * as an ordered list keyed by question ID so the frontend can render
 * them as columns when the user enables question view.</p>
 *
 * <p>Scores are always the effective (post-dispute) values. The original
 * scores are available on the full {@link SessionResultsResponseDTO} if needed.</p>
 */
@Getter
@Builder
public class ResultsTableRowDTO {
    private Long sessionId;
    private String interactionId;
    private LocalDateTime sessionDate;
    private Integer clientId;
    private String clientName;
    private Integer lobId;
    private String lobName;
    private Integer protocolId;
    private String protocolName;
    private Integer memberAuditedUserId;
    private String memberAuditedName;
    private Integer auditorUserId;
    private String auditorName;
    /** Effective CUSTOMER score (0 or 100, post-dispute). {@code null} if not yet calculated. */
    private Short customerScore;
    /** Effective BUSINESS score. */
    private Short businessScore;
    /** Effective COMPLIANCE score. */
    private Short complianceScore;
    private String auditStatus;
    /**
     * Per-question effective answers in question order.
     * Only populated when the caller requests question columns.
     */
    private List<QuestionAnswerDTO> questionAnswers;

    @Getter
    @Builder
    public static class QuestionAnswerDTO {
        private Integer questionId;
        private String questionText;
        private String category;
        /** Effective answer: YES, NO, or N/A (post-dispute if applicable). */
        private String effectiveAnswer;
        private String responseStatus;
    }
}
