package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Enriched response DTO used on the Session Results page and the Disputes inbox.
 *
 * <p>{@code isFlagged} has been added to let the frontend decide whether to show
 * the flag/unflag action button. QA-facing views should ignore this field —
 * flagging is an OPERATIONS-internal workflow state that has no meaning in QA
 * reports or the formal dispute audit trail.</p>
 */
@Getter
@Builder
public class AuditResponseResultDTO {
    private Long responseId;
    private Integer questionId;
    private String questionText;
    private String category;
    /** The answer recorded by the auditor at submission time. Never changes. */
    private String originalAnswer;
    /** The answer used for score calculation — overridden when dispute is MODIFIED. */
    private String effectiveAnswer;
    /** Formal dispute lifecycle: ANSWERED, DISPUTED, or RESOLVED. */
    private String responseStatus;
    /**
     * Whether this response has been informally flagged by an OPERATIONS user.
     * Distinct from {@code responseStatus} — flagging is temporary and internal.
     * QA-facing views should not render this field.
     */
    private Boolean isFlagged;
    /** {@code null} when no dispute has been raised against this response. */
    private AuditDisputeResponseDTO dispute;
}