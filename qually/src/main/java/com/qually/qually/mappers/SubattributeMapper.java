package com.qually.qually.mappers;

import com.qually.qually.dto.request.SubattributeRequestDTO;
import com.qually.qually.dto.response.SubattributeResponseDTO;
import com.qually.qually.models.AuditQuestion;
import com.qually.qually.models.Subattribute;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class SubattributeMapper {

    private final SubattributeOptionMapper optionMapper;

    public SubattributeMapper(SubattributeOptionMapper optionMapper) {
        this.optionMapper = optionMapper;
    }

    public SubattributeResponseDTO toDTO(Subattribute subattribute) {
        return SubattributeResponseDTO.builder()
                .subattributeId(subattribute.getSubattributeId())
                .subattributeText(subattribute.getSubattributeText())
                .isAccountabilitySubattribute(subattribute.isAccountability())
                .subattributeOptions(subattribute.getSubattributeOptions() != null
                        ? subattribute.getSubattributeOptions().stream()
                        .map(optionMapper::toDTO)
                        .toList()
                        : new ArrayList<>())
                .build();
    }

    public Subattribute toEntity(SubattributeRequestDTO dto, AuditQuestion parent) {
        if (dto == null) return null;

        Subattribute subattribute = Subattribute.builder()
                .subattributeText(dto.getSubattributeText())
                .isAccountability(dto.isAccountabilitySubattribute())
                .auditQuestion(parent)
                .subattributeOptions(new ArrayList<>())
                .build();

        if (dto.getSubattributeOptions() != null) {
            dto.getSubattributeOptions().forEach(oDto ->
                    subattribute.getSubattributeOptions().add(optionMapper.toEntity(oDto, subattribute))
            );
        }

        return subattribute;
    }
}