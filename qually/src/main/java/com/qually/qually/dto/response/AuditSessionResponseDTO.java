package com.qually.qually.dto.response;

import com.qually.qually.models.enums.AuditLogicType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditSessionResponseDTO {
    private Long sessionId;
    private String auditStatus;
    private String comments;
    private Integer protocolId;
    private String protocolName;
    private Integer protocolVersion;
    private String auditorEmail;
    private String auditorName;
    private AuditLogicType auditLogicType;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}