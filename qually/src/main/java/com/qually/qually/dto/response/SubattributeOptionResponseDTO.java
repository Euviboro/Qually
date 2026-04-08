package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubattributeOptionResponseDTO {
    private Long subattributeOptionId;
    private Integer subattributeId;
    private String optionLabel;
}
