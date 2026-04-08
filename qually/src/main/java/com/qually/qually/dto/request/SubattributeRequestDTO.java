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
    private String attributeText;

    @NotNull(message = "Question ID is required", groups = OnIndividualSave.class)
    private Integer questionId;

    private List<SubattributeOptionRequestDTO> subattributeOptions;
}