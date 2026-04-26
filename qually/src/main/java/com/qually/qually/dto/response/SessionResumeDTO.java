package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Minimal payload returned by {@code GET /api/sessions/{id}/resume}.
 *
 * <p>Contains only what the Log Session page needs to pre-populate its form
 * when a user resumes a DRAFT session — session metadata fields and the
 * previously recorded question answers with their subattribute selections.</p>
 *
 * <p>Scores are omitted — they are not calculated until the session is
 * submitted as COMPLETED.</p>
 */
@Getter
@Builder
public class SessionResumeDTO {

    private Long    sessionId;
    private String  interactionId;
    private Integer lobId;
    private Integer memberAuditedUserId;
    private String  comments;

    /** The answers previously recorded for this session, one entry per question. */
    private List<ResumeResponseItemDTO> responses;

    @Getter
    @Builder
    public static class ResumeResponseItemDTO {
        private Integer    questionId;
        /** "YES", "NO", or "N/A" — the answer stored at last save. */
        private String     questionAnswer;
        /**
         * Flat list of {@code subattribute_option_id} values the auditor
         * selected for this response. Empty when no subattribute options
         * were recorded (question had no sub-criteria or was not NO).
         */
        private List<Long> subattributeOptionIds;
    }
}