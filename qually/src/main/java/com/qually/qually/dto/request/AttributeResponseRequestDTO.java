package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttributeResponseRequestDTO {

    @NotNull(message = "Audit response ID is required")
    private Long auditResponseId;

    @NotNull(message = "Attribute ID is required")
    private Integer attributeId;

    @NotBlank(message = "Answer value is required")
    private String answerValue;
}