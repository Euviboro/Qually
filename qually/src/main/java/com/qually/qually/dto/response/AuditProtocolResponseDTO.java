package com.qually.qually.dto.response;

import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.ProtocolStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Response DTO for an {@link com.qually.qually.models.AuditProtocol}.
 *
 * <p>{@code auditLogicType} added — it is stored on the protocol, not the
 * session, and must be available to the frontend so the Log Session page can
 * display it as read-only context.</p>
 */
@Getter
@Builder
public class AuditProtocolResponseDTO {
    private Integer protocolId;
    private String protocolName;
    private Integer protocolVersion;
    private ProtocolStatus protocolStatus;
    /** Scoring strategy applied to all sessions that use this protocol. */
    private AuditLogicType auditLogicType;
    private Integer clientId;
    private String clientName;
    private List<AuditQuestionResponseDTO> auditQuestions;
}
