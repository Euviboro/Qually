package com.qually.qually.mappers;

import com.qually.qually.dto.response.SubattributeResponseDTO;
import com.qually.qually.dto.response.AuditProtocolResponseDTO;
import com.qually.qually.dto.response.AuditQuestionResponseDTO;
import com.qually.qually.dto.response.SubattributeOptionResponseDTO;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.AuditQuestion;
import com.qually.qually.models.Subattribute;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class AuditProtocolMapper {

    public AuditProtocolResponseDTO toDTO(AuditProtocol protocol) {
        if (protocol == null) return null;

        return AuditProtocolResponseDTO.builder()
                .protocolId(protocol.getProtocolId())
                .protocolName(protocol.getProtocolName())
                .protocolVersion(protocol.getProtocolVersion())
                .isFinalized(protocol.getIsFinalized())
                .clientId(protocol.getClient().getClientId())
                .clientName(protocol.getClient().getClientName())
                .auditQuestions(protocol.getAuditQuestions() != null ?
                        protocol.getAuditQuestions().stream()
                                .map(this::toQuestionDTO)
                                .toList() : new ArrayList<>())
                .build();
    }

    public AuditQuestionResponseDTO toQuestionDTO(AuditQuestion question) {
        return AuditQuestionResponseDTO.builder()
                .questionId(question.getQuestionId())
                .questionText(question.getQuestionText())
                .category(question.getCategory())
                .subattributes(question.getSubattributes() != null ?
                        question.getSubattributes().stream()
                                .map(this::toSubattributeDTO)
                                .toList() : new ArrayList<>())
                .build();
    }

    private SubattributeResponseDTO toSubattributeDTO(Subattribute subattribute) {
        return SubattributeResponseDTO.builder()
                .subattributeId(subattribute.getSubattributeId())
                .subattributeText(subattribute.getSubattributeText())
                // Now we are adding the deepest level: Options!
                .subattributeOptions(subattribute.getSubattributeOptions() != null ?
                        subattribute.getSubattributeOptions().stream()
                                .map(subattributeOption -> SubattributeOptionResponseDTO.builder()
                                        .subattributeOptionId(subattributeOption.getSubattributeOptionId())
                                        .optionLabel(subattributeOption.getOptionLabel())
                                        .build())
                                .toList() : new ArrayList<>())
                .build();
    }
}