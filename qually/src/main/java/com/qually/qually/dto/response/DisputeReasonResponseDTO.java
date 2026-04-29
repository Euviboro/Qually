package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for a {@link com.qually.qually.models.DisputeReason}.
 * Read-only reference data — reasons are seeded via SQL and never
 * created or updated through the API.
 */
@Getter
@Builder
public class DisputeReasonResponseDTO {
    private Integer reasonId;
    private String  reasonText;
}