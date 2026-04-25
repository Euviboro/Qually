package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for formally raising a dispute against an audit response.
 * Called by a Team Leader (or above in OPERATIONS) after reviewing a flagged response.
 */
@Getter
@Setter
public class AuditDisputeRequestDTO {

    @NotNull(message = "Response ID is required")
    private Long responseId;

    @NotNull(message = "Dispute reason is required")
    private Integer reasonId;

    /** Optional free-text context from the person raising the dispute. */
    private String disputeComment;
}
