package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents one interaction ID and its answers within a calibration round.
 *
 * <p>What is shown in {@code sessions} depends on the caller's role and
 * the round's state:</p>
 * <ul>
 *   <li>Round open, caller is participant — only the caller's own session
 *       is included (they cannot see others' answers before submitting)</li>
 *   <li>Round closed, caller is participant — only the caller's own session,
 *       with {@code expertAnswer} populated for side-by-side comparison</li>
 *   <li>Caller is QA manager — all sessions for all participants</li>
 * </ul>
 */
@Getter
@Builder
public class CalibrationGroupResponseDTO {

    private Long   groupId;
    private String interactionId;

    /**
     * Overall result for this interaction.
     * {@code null} — round still open.
     * {@code true} — all participants matched the expert.
     * {@code false} — at least one participant did not match.
     */
    private Boolean isCalibrated;

    /**
     * The expert's answer for this interaction.
     * {@code null} when the round is open — revealed only after closing.
     * The expert's name is never revealed to participants.
     */
    private String expertAnswer;

    /** Sessions visible to the caller — see class Javadoc for rules. */
    private List<CalibrationSessionResponseDTO> sessions;
}