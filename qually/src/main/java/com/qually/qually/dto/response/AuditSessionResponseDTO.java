package com.qually.qually.dto.response;

import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.ResolutionOutcome;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditSessionResponseDTO {
    private Long sessionId;
    private String auditStatus;
    private String interactionId;
    private String comments;
    private Integer protocolId;
    private String protocolName;
    private Integer protocolVersion;
    private AuditLogicType auditLogicType;
    private Integer clientId;
    private String clientName;
    private Integer auditorUserId;
    private String auditorName;
    private Integer memberAuditedUserId;
    private String memberAuditedName;
    private Integer lobId;
    private String lobName;
    private ResolutionOutcome resolutionOutcome;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}
