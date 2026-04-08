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
    private List<SubattributeOptionResponseDTO> subattributeOptions;
}