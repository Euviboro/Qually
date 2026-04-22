package com.qually.qually.mappers;

import com.qually.qually.dto.request.AuditProtocolRequestDTO;
import com.qually.qually.dto.response.AuditProtocolResponseDTO;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.Client;
import com.qually.qually.models.enums.ProtocolStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Mapper for {@link AuditProtocol} ↔ DTO conversions.
 *
 * <p>{@code auditLogicType} is now handled in both directions — it was added to
 * {@link AuditProtocol} to match the {@code audit_logic_type NOT NULL} column
 * in the {@code audit_protocols} table.</p>
 */
@Component
public class AuditProtocolMapper {

    private final AuditQuestionMapper auditQuestionMapper;

    public AuditProtocolMapper(AuditQuestionMapper auditQuestionMapper) {
        this.auditQuestionMapper = auditQuestionMapper;
    }

    /**
     * Maps a persisted {@link AuditProtocol} to its response DTO.
     *
     * @param protocol The managed entity. Must not be null.
     * @return The fully populated response DTO including questions and logic type.
     */
    public AuditProtocolResponseDTO toDTO(AuditProtocol protocol) {
        if (protocol == null) return null;

        return AuditProtocolResponseDTO.builder()
                .protocolId(protocol.getProtocolId())
                .protocolName(protocol.getProtocolName())
                .protocolVersion(protocol.getProtocolVersion())
                .protocolStatus(protocol.getProtocolStatus())
                .auditLogicType(protocol.getAuditLogicType())
                .clientId(protocol.getClient().getClientId())
                .clientName(protocol.getClient().getClientName())
                .auditQuestions(protocol.getAuditQuestions() != null
                        ? protocol.getAuditQuestions().stream()
                                .map(auditQuestionMapper::toDTO)
                                .toList()
                        : new ArrayList<>())
                .build();
    }

    /**
     * Maps an {@link AuditProtocolRequestDTO} to a new (unsaved) entity,
     * recursively building the question graph.
     *
     * @param dto    The incoming request payload.
     * @param parent The owning client entity.
     * @return A new, unsaved {@link AuditProtocol} with all children attached.
     */
    public AuditProtocol toEntity(AuditProtocolRequestDTO dto, Client parent) {
        if (dto == null) return null;

        AuditProtocol auditProtocol = AuditProtocol.builder()
                .protocolName(dto.getProtocolName())
                .protocolVersion(dto.getProtocolVersion())
                .protocolStatus(dto.getProtocolStatus() != null
                        ? dto.getProtocolStatus()
                        : ProtocolStatus.DRAFT)
                .auditLogicType(dto.getAuditLogicType())
                .client(parent)
                .auditQuestions(new ArrayList<>())
                .build();

        if (dto.getAuditQuestions() != null) {
            dto.getAuditQuestions().forEach(q ->
                    auditProtocol.getAuditQuestions().add(
                            auditQuestionMapper.toEntity(q, auditProtocol)));
        }
        return auditProtocol;
    }
}
