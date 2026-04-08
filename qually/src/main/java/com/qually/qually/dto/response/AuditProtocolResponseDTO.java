package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AuditProtocolResponseDTO {
    private Integer protocolId;
    private String protocolName;
    private Integer protocolVersion;
    private Boolean isFinalized;
    private Integer clientId;
    private String clientName;
    private List<AuditQuestionResponseDTO> auditQuestions;
}