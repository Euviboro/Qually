package com.qually.qually.mappers;

import com.qually.qually.dto.request.AuditSessionRequestDTO;
import com.qually.qually.dto.response.AuditSessionResponseDTO;
import com.qually.qually.models.*;
import com.qually.qually.models.enums.AuditStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper for {@link AuditSession} ↔ DTO conversions.
 *
 * <p><strong>Item 17 — defensive null-checks removed on required fields:</strong>
 * {@code auditProtocol} is declared {@code nullable = false} on the entity and
 * enforced by a NOT NULL constraint in the database. Null-checking it in the
 * mapper was masking potential data integrity issues — a session without a
 * protocol is a corrupted record that should fail loudly, not silently produce
 * a DTO with null protocol fields. The mapper now accesses {@code auditProtocol}
 * and its {@code client} directly without null guards.</p>
 *
 * <p>Fields that are legitimately nullable ({@code auditor},
 * {@code memberAuditedUser}, {@code lob}, {@code resolutionOutcome}) retain
 * their null-safe access patterns.</p>
 */
@Component
public class AuditSessionMapper {

    public AuditSessionResponseDTO toDTO(AuditSession session) {
        // auditProtocol is NOT NULL — access directly. A NullPointerException here
        // means a record was saved without a protocol, which is a data integrity
        // issue that should surface rather than be silently swallowed.
        AuditProtocol protocol = session.getAuditProtocol();
        Client         client  = protocol.getClient();

        // These are legitimately nullable — null-safe access is correct here.
        User auditor       = session.getAuditor();
        User memberAudited = session.getMemberAuditedUser();
        Lob  lob           = session.getLob();

        return AuditSessionResponseDTO.builder()
                .sessionId(session.getSessionId())
                .auditStatus(session.getAuditStatus().name())
                .interactionId(session.getInteractionId())
                .comments(session.getComments())
                .protocolId(protocol.getProtocolId())
                .protocolName(protocol.getProtocolName())
                .protocolVersion(protocol.getProtocolVersion())
                .auditLogicType(protocol.getAuditLogicType())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .auditorUserId(auditor != null ? auditor.getUserId() : null)
                .auditorName(auditor != null ? auditor.getFullName() : null)
                .memberAuditedUserId(memberAudited != null ? memberAudited.getUserId() : null)
                .memberAuditedName(memberAudited != null ? memberAudited.getFullName() : null)
                .lobId(lob != null ? lob.getLobId() : null)
                .lobName(lob != null ? lob.getLobName() : null)
                .resolutionOutcome(session.getResolutionOutcome())
                .startedAt(session.getStartedAt())
                .submittedAt(session.getSubmittedAt())
                .build();
    }

    public AuditSession toEntity(AuditSessionRequestDTO dto,
                                  AuditProtocol protocol,
                                  User auditor,
                                  User memberAudited,
                                  Lob lob) {
        AuditStatus status = dto.getAuditStatus() != null
                ? dto.getAuditStatus()
                : AuditStatus.DRAFT;

        LocalDateTime submittedAt = AuditStatus.COMPLETED.equals(status)
                ? LocalDateTime.now()
                : null;

        return AuditSession.builder()
                .auditProtocol(protocol)
                .auditor(auditor)
                .memberAuditedUser(memberAudited)
                .lob(lob)
                .interactionId(dto.getInteractionId())
                .comments(dto.getComments())
                .auditStatus(status)
                .submittedAt(submittedAt)
                .build();
    }
}
