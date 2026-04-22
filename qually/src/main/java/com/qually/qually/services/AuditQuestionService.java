package com.qually.qually.services;

import com.qually.qually.dto.request.AuditQuestionRequestDTO;
import com.qually.qually.dto.response.AuditQuestionResponseDTO;
import com.qually.qually.mappers.AuditQuestionMapper;
import com.qually.qually.mappers.SubattributeMapper;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.AuditQuestion;
import com.qually.qually.repositories.AuditProtocolRepository;
import com.qually.qually.repositories.AuditQuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for CRUD operations on {@link AuditQuestion} entities.
 *
 * <p>Both {@link #createQuestion} and {@link #updateQuestion} delegate
 * subattribute/option graph construction to {@link AuditQuestionMapper} and
 * {@link SubattributeMapper}, ensuring consistent behaviour: the mappers always
 * produce a fresh object graph from the DTO, and JPA's {@code orphanRemoval = true}
 * on the {@code subattributes} collection handles deletion of removed children.</p>
 */
@Service
public class AuditQuestionService {

    private final AuditQuestionRepository auditQuestionRepository;
    private final AuditProtocolRepository auditProtocolRepository;
    private final AuditQuestionMapper auditQuestionMapper;
    private final SubattributeMapper subattributeMapper;

    public AuditQuestionService(AuditQuestionRepository auditQuestionRepository,
                                AuditProtocolRepository auditProtocolRepository,
                                AuditQuestionMapper auditQuestionMapper,
                                SubattributeMapper subattributeMapper) {
        this.auditQuestionRepository = auditQuestionRepository;
        this.auditProtocolRepository = auditProtocolRepository;
        this.auditQuestionMapper = auditQuestionMapper;
        this.subattributeMapper = subattributeMapper;
    }

    /**
     * Creates a new {@link AuditQuestion} (with its nested subattributes and options)
     * under an existing protocol.
     *
     * @param dto Question payload. {@code protocolId} is required.
     * @return The persisted question as a response DTO.
     * @throws EntityNotFoundException if the referenced protocol does not exist.
     */
    @Transactional
    public AuditQuestionResponseDTO createQuestion(AuditQuestionRequestDTO dto) {
        AuditProtocol protocol = auditProtocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(dto.getProtocolId())));

        AuditQuestion question = auditQuestionMapper.toEntity(dto, protocol);
        return auditQuestionMapper.toDTO(auditQuestionRepository.save(question));
    }

    /**
     * Returns all questions, optionally scoped to a single protocol.
     *
     * @param protocolId When non-null, filters to questions belonging to this protocol.
     * @return List of matching questions including their subattributes and options.
     */
    @Transactional(readOnly = true)
    public List<AuditQuestionResponseDTO> getAllQuestions(Integer protocolId) {
        List<AuditQuestion> questions = (protocolId != null)
                ? auditQuestionRepository.findByAuditProtocol_ProtocolId(protocolId)
                : auditQuestionRepository.findAll();
        return questions.stream().map(auditQuestionMapper::toDTO).toList();
    }

    /**
     * Returns a single question by its ID.
     *
     * @param id Question ID.
     * @return The question DTO including subattributes and options.
     * @throws EntityNotFoundException if no question with the given ID exists.
     */
    @Transactional(readOnly = true)
    public AuditQuestionResponseDTO getQuestionById(Integer id) {
        return auditQuestionRepository.findById(id)
                .map(auditQuestionMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Question with ID %d not found".formatted(id)));
    }

    /**
     * Fully replaces an existing question's data, including its entire
     * subattribute and option graph.
     *
     * <p><strong>Strategy:</strong> scalar fields ({@code questionText},
     * {@code category}, {@code auditProtocol}) are set directly on the managed
     * entity. The subattribute collection is cleared first, then rebuilt from
     * the DTO using the same mapper method used during creation. Because
     * {@code orphanRemoval = true} is set on the {@code subattributes}
     * relationship, JPA automatically deletes any previously existing children
     * that are no longer referenced.</p>
     *
     * <p><strong>Fixed bugs vs. previous implementation:</strong></p>
     * <ul>
     *   <li>The old code called {@code question.getSubattributes().clear()} inside
     *       the loop, which meant every subattribute after the first was being
     *       added to an already-cleared list, producing only the last entry.</li>
     *   <li>The old code bypassed {@code auditQuestionMapper} and used a private
     *       {@code toDTO} method that omitted subattributes from the response,
     *       causing {@code getAllQuestions} and {@code getQuestionById} to return
     *       incomplete data after an update.</li>
     * </ul>
     *
     * @param id  Question ID to update.
     * @param dto Full updated payload. {@code protocolId} is required.
     * @return The updated question DTO with all subattributes and options.
     * @throws EntityNotFoundException if the question or protocol does not exist.
     */
    @Transactional
    public AuditQuestionResponseDTO updateQuestion(Integer id, AuditQuestionRequestDTO dto) {
        AuditQuestion question = auditQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Question with ID %d not found".formatted(id)));
        AuditProtocol protocol = auditProtocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(dto.getProtocolId())));

        // Update scalar fields
        question.setQuestionText(dto.getQuestionText());
        question.setCategory(dto.getCategory());
        question.setAuditProtocol(protocol);

        // Rebuild subattribute graph: clear first (outside the loop!), then
        // re-add all entries from the DTO using the mapper for consistency.
        question.getSubattributes().clear();

        if (dto.getSubattributes() != null) {
            dto.getSubattributes().forEach(sDto ->
                    question.getSubattributes().add(
                            subattributeMapper.toEntity(sDto, question)
                    )
            );
        }

        return auditQuestionMapper.toDTO(auditQuestionRepository.save(question));
    }
}
