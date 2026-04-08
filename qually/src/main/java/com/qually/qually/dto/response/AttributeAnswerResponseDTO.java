package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AttributeAnswerResponseDTO {
    private Long attributeResponseId;
    private Long auditResponseId;
    private Integer attributeId;
    private String attributeText;
    private String answerValue;
}