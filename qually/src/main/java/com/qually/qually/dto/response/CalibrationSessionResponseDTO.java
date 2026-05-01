package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Represents one participant's answer for one interaction in a calibration round.
 *
 * <p>The expert's identity is never exposed in this DTO — participants see
 * only answers and results, not who the expert is. QA managers who need
 * to know the expert identity read it from
 * {@link CalibrationParticipantResponseDTO#isExpert()} which is only
 * populated in the manager view.</p>
 */
@Getter
@Builder
public class CalibrationSessionResponseDTO {

    private Long          calibrationSessionId;
    private Integer       userId;
    private String        userFullName;
    private String        calibrationAnswer;

    /**
     * Whether this participant's answer matched the expert's.
     * {@code null} when the round is still open — comparison has not run yet.
     * {@code true} / {@code false} after the round is closed and compared.
     */
    private Boolean       isCalibrated;

    /**
     * The expert's answer for this interaction — shown side-by-side with
     * the participant's own answer after the round closes.
     * {@code null} when the round is still open.
     * Only populated in the caller's own session view — other participants'
     * expert answer field is also null to prevent cross-participant leakage.
     */
    private String        expertAnswer;

    private LocalDateTime answeredAt;
}