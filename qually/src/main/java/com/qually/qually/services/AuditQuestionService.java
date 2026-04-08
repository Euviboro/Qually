package com.qually.qually.services;

import com.qually.qually.dto.request.AuditQuestionRequestDTO;
import com.qually.qually.dto.request.SubattributeOptionRequestDTO;
import com.qually.qually.dto.request.SubattributeRequestDTO;
import com.qually.qually.dto.response.AuditQuestionResponseDTO;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.AuditQuestion;
import com.qually.qually.models.Subattribute;
import com.qually.qually.models.SubattributeOption;
import com.qually.qually.repositories.AuditProtocolRepository;
import com.qually.qually.repositories.AuditQuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditQuestionService {

    private final AuditQuestionRepository auditQuestionRepository;
    private final AuditProtocolRepository auditProtocolRepository;

    public AuditQuestionService(AuditQuestionRepository auditQuestionRepository,
                                AuditProtocolRepository auditProtocolRepository) {
        this.auditQuestionRepository = auditQuestionRepository;
        this.auditProtocolRepository = auditProtocolRepository;
    }

    @Transactional
    public AuditQuestionResponseDTO createQuestion(AuditQuestionRequestDTO dto) {
        AuditProtocol protocol = auditProtocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException("Protocol with ID %d not found".formatted(dto.getProtocolId())));

        AuditQuestion question = AuditQuestion.builder()
                .questionText(dto.getQuestionText())
                .category(dto.getCategory())
                .auditProtocol(protocol)
                .subattributes(new ArrayList<>())
                .build();

        if (dto.getSubattributes() != null) {
            for (SubattributeRequestDTO sattrDto : dto.getSubattributes()) {
                Subattribute subattribute = Subattribute.builder()
                        .subattributeText(sattrDto.getAttributeText())
                        .auditQuestion(question)
                        .subattributeOptions(new ArrayList<>())
                        .build();

                if (sattrDto.getSubattributeOptions() != null) {
                    for (SubattributeOptionRequestDTO sattrOptionDto : sattrDto.getSubattributeOptions()) {
                        SubattributeOption subattributeOption = SubattributeOption.builder()
                                .optionLabel(sattrOptionDto.getOptionLabel())
                                .subattribute(subattribute)
                                .build();
                        subattribute.getSubattributeOptions().add(subattributeOption);
                    }
                }
                question.getSubattributes().add(subattribute);
            }
        }

        return toDTO(auditQuestionRepository.save(question));
    }

    @Transactional(readOnly = true)
    public List<AuditQuestionResponseDTO> getAllQuestions(Integer protocolId) {
        List<AuditQuestion> questions = (protocolId != null)
                ? auditQuestionRepository.findByAuditProtocol_ProtocolId(protocolId)
                : auditQuestionRepository.findAll();
        return questions.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public AuditQuestionResponseDTO getQuestionById(Integer id) {
        return auditQuestionRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Question with ID %d not found".formatted(id)));
    }

    @Transactional
    public AuditQuestionResponseDTO updateQuestion(Integer id, AuditQuestionRequestDTO dto) {
        AuditQuestion question = auditQuestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question with ID %d not found".formatted(id)));
        AuditProtocol protocol = auditProtocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException("Protocol with ID %d not found".formatted(dto.getProtocolId())));

        question.setQuestionText(dto.getQuestionText());
        question.setCategory(dto.getCategory());
        question.setAuditProtocol(protocol);

        if (dto.getSubattributes() != null) {
            for (SubattributeRequestDTO sattrDto : dto.getSubattributes()) {
                Subattribute subattribute = Subattribute.builder()
                        .subattributeText(sattrDto.getAttributeText())
                        .auditQuestion(question)
                        .subattributeOptions(new ArrayList<>())
                        .build();

                if (sattrDto.getSubattributeOptions() != null) {
                    for (SubattributeOptionRequestDTO optDto : sattrDto.getSubattributeOptions()) {
                        SubattributeOption option = SubattributeOption.builder()
                                .optionLabel(optDto.getOptionLabel())
                                .subattribute(subattribute)
                                .build();
                        subattribute.getSubattributeOptions().add(option);
                    }
                }
                question.getSubattributes().add(subattribute);
            }
        }

        return toDTO(auditQuestionRepository.save(question));
    }

    private AuditQuestionResponseDTO toDTO(AuditQuestion question) {
        return AuditQuestionResponseDTO.builder()
                .questionId(question.getQuestionId())
                .questionText(question.getQuestionText())
                .category(question.getCategory())
                .protocolId(question.getAuditProtocol().getProtocolId())
                .protocolName(question.getAuditProtocol().getProtocolName())
                .build();
    }
}