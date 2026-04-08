package com.qually.qually.dto.response;

import com.qually.qually.models.enums.CopcCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AuditQuestionResponseDTO {
    private Integer questionId;
    private String questionText;
    private CopcCategory category;
    private Integer protocolId;
    private String protocolName;
    private List<SubattributeResponseDTO> subattributes;
}