package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SubattributeResponseDTO {
    private Integer subattributeId;
    private String subattributeText;
    private Integer questionId;

    /**
     * Whether this subattribute is the accountability selector for its parent question.
     * The frontend uses this to render the accountability badge and apply required-field
     * validation when the parent question is answered NO in an ACCOUNTABILITY protocol.
     */
    private boolean isAccountabilitySubattribute;

    private List<SubattributeOptionResponseDTO> subattributeOptions;
}