package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubattributeOptionResponseDTO {
    private Long subattributeOptionId;
    private Integer subattributeId;
    private String optionLabel;

    /**
     * Whether selecting this option means the company is accountable for the failure.
     * The frontend uses this to render a visual indicator on the option chip
     * so auditors know which choice triggers a score failure.
     */
    private boolean isCompanyAccountable;
}