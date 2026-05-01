package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Summary of a participant enrolled in a calibration round.
 *
 * <p>{@code isExpert} is only populated when the caller is a QA manager —
 * participants do not see who the expert is.</p>
 *
 * <p>{@code hasAnsweredAll} is derived from whether the participant has
 * submitted a {@link com.qually.qually.models.CalibrationSession} for
 * every {@link com.qually.qually.models.CalibrationGroup} in the round.</p>
 */
@Getter
@Builder
public class CalibrationParticipantResponseDTO {

    private Integer userId;
    private String  fullName;
    private String  roleName;

    /**
     * Whether this participant is the expert.
     * {@code null} when the caller is a regular participant — the expert's
     * identity is hidden. Populated (true/false) only in the QA manager view.
     */
    private Boolean isExpert;

    /**
     * Whether the participant has submitted answers for all interactions
     * in the round. Derived field — not stored in the database.
     */
    private Boolean hasAnsweredAll;

    /**
     * Number of interactions answered out of the total in the round.
     * e.g. "2 / 3". Shown in the manager view to track completion.
     */
    private Integer answeredCount;
    private Integer totalCount;
}