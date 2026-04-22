package com.qually.qually.mappers;

import com.qually.qually.dto.request.AuditSessionRequestDTO;
import com.qually.qually.dto.response.AuditSessionResponseDTO;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.AuditSession;
import com.qually.qually.models.User;
import com.qually.qually.models.enums.AuditStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper for {@link AuditSession} ↔ DTO conversions.
 *
 * <p>Follows the same pattern as {@link AuditProtocolMapper} and
 * {@link AuditQuestionMapper}: the service resolves all entity references
 * (protocol, auditor) before calling the mapper, keeping the mapper free of
 * repository dependencies.</p>
 *
 * <p>{@code auditLogicType} is exposed in responses even though it lives on
 * the protocol — it is read from {@code session.getAuditProtocol()} so the
 * client always has the scoring context alongside the session data without a
 * separate protocol fetch.</p>
 */
@Component
public class AuditSessionMapper {

    /**
     * Maps a persisted {@link AuditSession} to its response DTO.
     *
     * <p>{@code auditLogicType} is sourced from
     * {@code session.getAuditProtocol().getAuditLogicType()} — it is a
     * protocol-level field that no longer lives on the session entity.</p>
     *
     * @param session The managed entity. Must not be null.
     * @return The fully populated response DTO.
     */
    public AuditSessionResponseDTO toDTO(AuditSession session) {
        AuditProtocol protocol = session.getAuditProtocol();
        User auditor = session.getAuditor();

        return AuditSessionResponseDTO.builder()
                .sessionId(session.getSessionId())
                .auditStatus(session.getAuditStatus().name())
                .interactionId(session.getInteractionId())
                .memberAudited(session.getMemberAudited())
                .comments(session.getComments())
                .protocolId(protocol != null ? protocol.getProtocolId() : null)
                .protocolName(protocol != null ? protocol.getProtocolName() : null)
                .protocolVersion(protocol != null ? protocol.getProtocolVersion() : null)
                // auditLogicType lives on the protocol, not the session
                .auditLogicType(protocol != null ? protocol.getAuditLogicType() : null)
                .auditorUserId(auditor != null ? auditor.getUserId() : null)
                .auditorName(auditor != null ? auditor.getFullName() : null)
                .resolutionOutcome(session.getResolutionOutcome())
                .startedAt(session.getStartedAt())
                .submittedAt(session.getSubmittedAt())
                .build();
    }

    /**
     * Maps an {@link AuditSessionRequestDTO} to a new (unsaved) entity.
     *
     * <p>The service is responsible for resolving {@code protocol} and
     * {@code auditor} before calling this method — the same contract used by
     * {@link AuditProtocolMapper#toEntity(com.qually.qually.dto.request.AuditProtocolRequestDTO, com.qually.qually.models.Client)}.</p>
     *
     * <p>Status defaulting and {@code submittedAt} auto-assignment are handled
     * here so the service stays clean:</p>
     * <ul>
     *   <li>{@code null} status defaults to {@link AuditStatus#DRAFT}.</li>
     *   <li>{@link AuditStatus#COMPLETED} triggers an automatic
     *       {@code submittedAt = now()}.</li>
     * </ul>
     *
     * @param dto     The incoming request payload.
     * @param protocol The resolved protocol entity.
     * @param auditor  The resolved auditor entity.
     * @return A new, unsaved {@link AuditSession}.
     */
    public AuditSession toEntity(AuditSessionRequestDTO dto,
                                  AuditProtocol protocol,
                                  User auditor) {
        AuditStatus status = (dto.getAuditStatus() != null)
                ? dto.getAuditStatus()
                : AuditStatus.DRAFT;

        LocalDateTime submittedAt = AuditStatus.COMPLETED.equals(status)
                ? LocalDateTime.now()
                : null;

        return AuditSession.builder()
                .auditProtocol(protocol)
                .auditor(auditor)
                .interactionId(dto.getInteractionId())
                .memberAudited(dto.getMemberAudited())
                .comments(dto.getComments())
                .auditStatus(status)
                .submittedAt(submittedAt)
                .build();
    }
}
