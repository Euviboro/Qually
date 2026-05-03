package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.transaction.annotation.Transactional;

@Getter
@Builder
@Transactional
public class SubattributeOptionResponseDTO {
    private Long subattributeOptionId;
    private Integer subattributeId;
    private String optionLabel;
}
