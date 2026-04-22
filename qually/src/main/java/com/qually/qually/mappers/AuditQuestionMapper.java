package com.qually.qually.mappers;

import com.qually.qually.dto.request.AuditQuestionRequestDTO;
import com.qually.qually.dto.response.AuditQuestionResponseDTO;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.AuditQuestion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class AuditQuestionMapper {

    private final SubattributeMapper subattributeMapper;

    public AuditQuestionMapper(SubattributeMapper subattributeMapper) {
        this.subattributeMapper = subattributeMapper;
    }

    /**
     * Maps a persisted {@link AuditQuestion} entity to its response DTO.
     *
     * <p><strong>Fix:</strong> {@code protocolId} and {@code protocolName} were
     * previously not populated. Because this mapper is called both for standalone
     * question endpoints and when questions are nested inside a protocol response
     * (via {@code AuditProtocolMapper}), omitting these fields meant the frontend
     * always received {@code protocolId: null} for questions fetched as part of a
     * protocol. Any subsequent PUT to {@code /questions/:id} sent a null
     * {@code protocolId}, causing {@code findById(null)} in the service to throw
     * {@code IllegalArgumentException}, which surfaced as a 500 "An unexpected
     * error occurred".</p>
     *
     * @param auditQuestion The managed entity to map. Must not be null.
     * @return The fully populated response DTO including protocol context.
     */
    public AuditQuestionResponseDTO toDTO(AuditQuestion auditQuestion) {
        // Resolve the parent protocol once so both fields read from the same object.
        AuditProtocol protocol = auditQuestion.getAuditProtocol();

        return AuditQuestionResponseDTO.builder()
                .questionId(auditQuestion.getQuestionId())
                .questionText(auditQuestion.getQuestionText())
                .category(auditQuestion.getCategory())
                // FIX: these two fields were missing, leaving protocolId = null in
                // every response. The frontend uses protocolId to build the PUT body;
                // null caused findById(null) → IllegalArgumentException → 500.
                .protocolId(protocol != null ? protocol.getProtocolId() : null)
                .protocolName(protocol != null ? protocol.getProtocolName() : null)
                .subattributes(auditQuestion.getSubattributes() != null
                        ? auditQuestion.getSubattributes().stream()
                        .map(subattributeMapper::toDTO)
                        .toList()
                        : new ArrayList<>())
                .build();
    }

    /**
     * Maps an {@link AuditQuestionRequestDTO} to a new (unsaved) entity,
     * recursively building the subattribute and option graph.
     *
     * @param dto    The incoming request payload. Returns null when null.
     * @param parent The owning protocol entity. Set on the entity and on every
     *               nested subattribute via the subattribute mapper.
     * @return A new, unsaved {@link AuditQuestion} with all children attached.
     */
    public AuditQuestion toEntity(AuditQuestionRequestDTO dto, AuditProtocol parent) {
        if (dto == null) return null;

        AuditQuestion auditQuestion = AuditQuestion.builder()
                .questionText(dto.getQuestionText())
                .category(dto.getCategory())
                .auditProtocol(parent)
                .subattributes(new ArrayList<>())
                .build();

        if (dto.getSubattributes() != null) {
            dto.getSubattributes().forEach(sDto ->
                    auditQuestion.getSubattributes().add(
                            subattributeMapper.toEntity(sDto, auditQuestion)
                    )
            );
        }

        return auditQuestion;
    }
}