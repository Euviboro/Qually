package com.qually.qually.dto.request;

import com.qually.qually.models.enums.ResolutionOutcome;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for resolving an existing dispute.
 * Submitted by a QA user (auditor's manager or above).
 */
@Getter
@Setter
public class ResolveDisputeRequestDTO {

    @NotNull(message = "Resolution outcome is required")
    private ResolutionOutcome resolutionOutcome;

    /**
     * The corrected answer selected by the QA reviewer.
     * Required when {@code resolutionOutcome = MODIFIED}.
     * Must be one of: {@code "YES"}, {@code "NO"}, {@code "N/A"}.
     * Must differ from the original answer — the frontend enforces this.
     */
    private String newAnswer;

    /** Optional explanation of the resolution decision. */
    private String resolutionNote;
}
