package com.qually.qually.services;

import com.qually.qually.dto.request.SubattributeRequestDTO;
import com.qually.qually.dto.response.SubattributeResponseDTO;
import com.qually.qually.mappers.SubattributeMapper;
import com.qually.qually.models.Subattribute;
import com.qually.qually.models.AuditQuestion;
import com.qually.qually.repositories.SubattributeRepository;
import com.qually.qually.repositories.AuditQuestionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubattributeService {

    private final SubattributeRepository subattributeRepository;
    private final AuditQuestionRepository auditQuestionRepository;
    private final SubattributeMapper subattributeMapper;

    public SubattributeService(SubattributeRepository subattributeRepository, AuditQuestionRepository auditQuestionRepository, SubattributeMapper subattributeMapper) {
        this.subattributeRepository = subattributeRepository;
        this.auditQuestionRepository = auditQuestionRepository;
        this.subattributeMapper = subattributeMapper;
    }

    @Transactional
    public SubattributeResponseDTO createSubattribute(SubattributeRequestDTO dto) {
        AuditQuestion question = auditQuestionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new EntityNotFoundException("Question with ID %d not found".formatted(dto.getQuestionId())));

        Subattribute subattribute = subattributeMapper.toEntity(dto, question);

        return subattributeMapper.toDTO(subattributeRepository.save(subattribute));
    }

    @Transactional(readOnly = true)
    public SubattributeResponseDTO getAttributeById(Integer id) {
        return subattributeRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Attribute with ID " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<SubattributeResponseDTO> getAttributesByQuestionId(Integer questionId) {
        return subattributeRepository.findByAuditQuestion_QuestionId(questionId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public SubattributeResponseDTO updateAttribute(Integer id, SubattributeRequestDTO dto) {
        Subattribute subattribute = subattributeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attribute with ID " + id + " not found"));
        AuditQuestion question = auditQuestionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new EntityNotFoundException("Question with ID " + dto.getQuestionId() + " not found"));

        subattribute.setSubattributeText(dto.getSubattributeText());
        subattribute.setAuditQuestion(question);
        return toDTO(subattributeRepository.save(subattribute));
    }

    private SubattributeResponseDTO toDTO(Subattribute subattribute) {
        return SubattributeResponseDTO.builder()
                .subattributeId(subattribute.getSubattributeId())
                .subattributeText(subattribute.getSubattributeText())
                .questionId(subattribute.getAuditQuestion().getQuestionId())
                .build();
    }
}