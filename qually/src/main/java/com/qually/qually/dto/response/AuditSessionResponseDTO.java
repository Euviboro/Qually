package com.qually.qually.dto.response;

import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.ResolutionOutcome;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response DTO for an {@link com.qually.qually.models.AuditSession}.
 *
 * <p><strong>Schema alignment changes:</strong></p>
 * <ul>
 *   <li>{@code auditorEmail} replaced by {@code auditorUserId} (Integer) and
 *       {@code auditorName} — the auditor FK is now on {@code user_id}.</li>
 *   <li>{@code memberAudited} added — the person whose work was audited.</li>
 *   <li>{@code interactionId} added — was on the entity but never exposed
 *       in previous responses.</li>
 *   <li>{@code auditLogicType} is still included in the response but is now
 *       populated from {@code session.getAuditProtocol().getAuditLogicType()}
 *       in the mapper, since the column lives on the protocol.</li>
 *   <li>{@code resolutionOutcome} added.</li>
 * </ul>
 */
@Getter
@Builder
public class AuditSessionResponseDTO {
    private Long sessionId;
    private String auditStatus;
    private String interactionId;
    private String memberAudited;
    private String comments;
    private Integer protocolId;
    private String protocolName;
    private Integer protocolVersion;
    /** Read from {@code session.auditProtocol.auditLogicType} — not stored on the session. */
    private AuditLogicType auditLogicType;
    private Integer auditorUserId;
    private String auditorName;
    private ResolutionOutcome resolutionOutcome;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}
