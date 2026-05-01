package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full response for a calibration round.
 *
 * <p>Used for both the list view (where {@code groups} may be omitted
 * for performance) and the detail view (where everything is included).</p>
 *
 * <p>What participants see vs. what QA managers see is controlled by the
 * service layer before mapping — this DTO carries whatever the service
 * decides to expose.</p>
 */
@Getter
@Builder
public class CalibrationRoundResponseDTO {

    private Long          roundId;
    private String        roundName;
    private Integer       clientId;
    private String        clientName;
    private Integer       protocolId;
    private String        protocolName;
    private Integer       questionId;
    private String        questionText;
    private String        category;

    /**
     * Whether the round is still accepting answers.
     * {@code true} — open, answers can be submitted.
     * {@code false} — closed, results are finalised.
     */
    private Boolean       isOpen;

    /**
     * Overall calibration result.
     * {@code null} — round still open.
     * {@code true} — all participants passed all interactions.
     * {@code false} — at least one failure.
     */
    private Boolean       isCalibrated;

    private String        createdByName;
    private LocalDateTime createdAt;

    /**
     * Interaction groups — populated in the detail view.
     * {@code null} or empty in the list view for performance.
     */
    private List<CalibrationGroupResponseDTO> groups;

    /**
     * Enrolled participants — populated in the detail view.
     * {@code isExpert} on each participant is only visible to QA managers.
     */
    private List<CalibrationParticipantResponseDTO> participants;

    /**
     * Convenience field — how many interactions the current caller
     * has already answered. Derived in the service layer.
     */
    private Integer callerAnsweredCount;
    private Integer totalGroupCount;
}