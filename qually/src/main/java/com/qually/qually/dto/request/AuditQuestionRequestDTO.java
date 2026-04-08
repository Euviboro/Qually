package com.qually.qually.dto.request;

import com.qually.qually.groups.OnDeepSave;
import com.qually.qually.groups.OnIndividualSave;
import com.qually.qually.models.enums.CopcCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AuditQuestionRequestDTO {

/*    @NotBlank(message = "Question ID is required", groups = {OnIndividualSave.class})
    private String questionId;
*/
    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotNull(message = "Category is required", groups = {OnDeepSave.class, OnIndividualSave.class})
    private CopcCategory category;

    @NotNull(message = "Protocol ID is required", groups = {OnIndividualSave.class})
    private Integer protocolId;

    private List<SubattributeRequestDTO> subattributes;
}