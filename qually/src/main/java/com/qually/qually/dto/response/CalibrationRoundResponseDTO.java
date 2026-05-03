package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full response for a calibration round.
 *
 * <p>{@code callerRole} tells the frontend exactly how to render the page
 * without re-deriving it from other fields:</p>
 * <ul>
 *   <li>{@code SR_QA}      — direct manager of the round creator. Sees all
 *       participants' answers, has Close Calibration button.</li>
 *   <li>{@code CREATOR}    — QA Specialist who created the round. Sees own
 *       answers + participant completion list (no other answers).</li>
 *   <li>{@code EXPERT}     — the designated expert. Answers like a regular
 *       participant — no special UI treatment.</li>
 *   <li>{@code PARTICIPANT} — everyone else enrolled in the round.</li>
 * </ul>
 *
 * <p>{@code isManagerParticipant} is {@code true} only when the caller is
 * {@code SR_QA} AND is also enrolled as a participant. In that case the
 * detail page renders two sections: their participant section and the
 * full manager section.</p>
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
    private Boolean       isOpen;
    private Boolean       isCalibrated;
    private String        createdByName;
    private LocalDateTime createdAt;

    /**
     * The caller's role in this round — one of SR_QA, CREATOR, EXPERT, PARTICIPANT.
     * Set by the service before mapping. Never null for enrolled users.
     */
    private String callerRole;

    /**
     * True when the caller is SR_QA AND is also enrolled as a participant.
     * Signals the detail page to render both a participant section and a
     * manager section.
     */
    private Boolean isManagerParticipant;

    private List<CalibrationGroupResponseDTO>       groups;
    private List<CalibrationParticipantResponseDTO> participants;

    /** How many interactions the caller has answered. Null in manager-only view. */
    private Integer callerAnsweredCount;
    private Integer totalGroupCount;
}