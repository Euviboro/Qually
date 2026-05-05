package com.qually.qually.dto.request;

import com.qually.qually.groups.OnIndividualSave;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class SubattributeRequestDTO {

    @NotBlank(message = "Attribute text is required")
    private String subattributeText;

    @NotNull(message = "Question ID is required", groups = OnIndividualSave.class)
    private Integer questionId;

    /**
     * Marks this subattribute as the accountability selector.
     * At most one subattribute per question should have this set to {@code true}.
     * Ignored in STANDARD protocols; required for accountability scoring in
     * ACCOUNTABILITY protocols.
     */
    private boolean isAccountabilitySubattribute;

    private List<SubattributeOptionRequestDTO> subattributeOptions;
}