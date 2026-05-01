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
 */
@Component
public class AuditProtocolMapper {

    private final AuditQuestionMapper auditQuestionMapper;

    public AuditProtocolMapper(AuditQuestionMapper auditQuestionMapper) {
        this.auditQuestionMapper = auditQuestionMapper;
    }

    public AuditProtocolResponseDTO toDTO(AuditProtocol protocol) {
        if (protocol == null) return null;

        return AuditProtocolResponseDTO.builder()
                .protocolId(protocol.getProtocolId())
                .protocolName(protocol.getProtocolName())
                .protocolAbbreviation(protocol.getProtocolAbbreviation())
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

    public AuditProtocol toEntity(AuditProtocolRequestDTO dto, Client parent) {
        if (dto == null) return null;

        AuditProtocol auditProtocol = AuditProtocol.builder()
                .protocolName(dto.getProtocolName())
                .protocolAbbreviation(dto.getProtocolAbbreviation() != null
                        ? dto.getProtocolAbbreviation().toUpperCase().trim()
                        : null)
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