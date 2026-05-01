package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for {@code POST /api/calibration/groups/{groupId}/answer}.
 *
 * <p>The submitting user is sourced from the JWT security context.
 * Answers are immutable — submitting twice for the same group returns 400.</p>
 */
@Getter
@Setter
public class SubmitAnswerRequestDTO {

    /**
     * The participant's answer for this interaction.
     * Valid values: {@code "YES"}, {@code "NO"}, {@code "N/A"}.
     * Validated against {@link com.qually.qually.models.enums.AuditAnswerType}
     * in the service layer.
     */
    @NotBlank(message = "Answer is required")
    private String calibrationAnswer;
}