package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for a subattribute response.
 *
 * <p>Updated to reflect the new {@code subattribute_responses} schema which
 * links to {@code subattribute_options} rather than storing a free-text answer.
 * The subattribute text and option label are derived by joining through the
 * option FK — nothing is stored redundantly.</p>
 */
@Getter
@Builder
public class AttributeAnswerResponseDTO {
    private Long subattributeResponseId;
    private Long auditResponseId;
    private Integer subattributeId;
    private String subattributeText;
    private Long optionId;
    private String optionLabel;
}
