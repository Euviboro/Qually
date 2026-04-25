package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Enriched response DTO used exclusively on the Session Results page.
 *
 * <p>Includes both the original answer (immutable) and the effective answer
 * (which may differ if a dispute was resolved as MODIFIED). The frontend
 * displays the effective answer for scoring context and the original for
 * transparency in the dispute history.</p>
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
    private String responseStatus;
    /** {@code null} when no dispute has been raised against this response. */
    private AuditDisputeResponseDTO dispute;
}
