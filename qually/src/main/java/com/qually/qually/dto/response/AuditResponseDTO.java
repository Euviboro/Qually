package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditResponseDTO {
    private Long auditResponseId;
    private Long sessionId;
    private Integer questionId;
    private String questionText;
    private String questionAnswer;
}