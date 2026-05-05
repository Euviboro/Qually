package com.qually.qually.mappers;

import com.qually.qually.dto.request.SubattributeOptionRequestDTO;
import com.qually.qually.dto.response.SubattributeOptionResponseDTO;
import com.qually.qually.models.Subattribute;
import com.qually.qually.models.SubattributeOption;
import org.springframework.stereotype.Component;

@Component
public class SubattributeOptionMapper {

    public SubattributeOptionResponseDTO toDTO(SubattributeOption subattributeOption) {
        return SubattributeOptionResponseDTO.builder()
                .subattributeOptionId(subattributeOption.getSubattributeOptionId())
                .subattributeId(subattributeOption.getSubattribute() != null
                        ? subattributeOption.getSubattribute().getSubattributeId()
                        : null)
                .optionLabel(subattributeOption.getOptionLabel())
                .isCompanyAccountable(subattributeOption.isCompanyAccountable())
                .build();
    }

    public SubattributeOption toEntity(SubattributeOptionRequestDTO dto, Subattribute parent) {
        if (dto == null) return null;

        return SubattributeOption.builder()
                .subattribute(parent)
                .optionLabel(dto.getOptionLabel())
                .isCompanyAccountable(dto.isCompanyAccountable())
                .build();
    }
}