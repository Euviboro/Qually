package com.qually.qually.services;

import com.qually.qually.dto.request.AttributeResponseRequestDTO;
import com.qually.qually.dto.response.AttributeAnswerResponseDTO;
import com.qually.qually.models.Subattribute;
import com.qually.qually.models.SubattributeResponse;
import com.qually.qually.models.AuditResponse;
import com.qually.qually.repositories.SubattributeRepository;
import com.qually.qually.repositories.AttributeResponseRepository;
import com.qually.qually.repositories.AuditResponseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AttributeResponseService {

    private final AttributeResponseRepository attributeResponseRepository;
    private final AuditResponseRepository auditResponseRepository;
    private final SubattributeRepository subattributeRepository;

    public AttributeResponseService(AttributeResponseRepository attributeResponseRepository,
                                    AuditResponseRepository auditResponseRepository,
                                    SubattributeRepository subattributeRepository) {
        this.attributeResponseRepository = attributeResponseRepository;
        this.auditResponseRepository = auditResponseRepository;
        this.subattributeRepository = subattributeRepository;
    }

    @Transactional
    public AttributeAnswerResponseDTO createAttributeResponse(AttributeResponseRequestDTO dto) {
        AuditResponse auditResponse = auditResponseRepository.findById(dto.getAuditResponseId())
                .orElseThrow(() -> new EntityNotFoundException("Audit response with ID %d not found".formatted(dto.getAuditResponseId())));
        Subattribute subattribute = subattributeRepository.findById(dto.getAttributeId())
                .orElseThrow(() -> new EntityNotFoundException("Attribute with ID %s not found".formatted(dto.getAttributeId())));

        SubattributeResponse subattributeResponse = SubattributeResponse.builder()
                .auditResponse(auditResponse)
                .subattribute(subattribute)
                .answerValue(dto.getAnswerValue())
                .build();

        return toDTO(attributeResponseRepository.save(subattributeResponse));
    }

    @Transactional(readOnly = true)
    public List<AttributeAnswerResponseDTO> getAttributeResponsesByAuditResponse(Long auditResponseId) {
        return attributeResponseRepository.findByAuditResponse_AuditResponseId(auditResponseId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private AttributeAnswerResponseDTO toDTO(SubattributeResponse subattributeResponse) {
        return AttributeAnswerResponseDTO.builder()
                .attributeResponseId(subattributeResponse.getAttributeResponseId())
                .auditResponseId(subattributeResponse.getAuditResponse().getAuditResponseId())
                .attributeId(subattributeResponse.getSubattribute().getSubattributeId())
                .attributeText(subattributeResponse.getSubattribute().getSubattributeText())
                .answerValue(subattributeResponse.getAnswerValue())
                .build();
    }
}