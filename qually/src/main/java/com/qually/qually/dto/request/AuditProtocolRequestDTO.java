package com.qually.qually.dto.request;

import com.qually.qually.groups.OnIndividualSave;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AuditProtocolRequestDTO {

    @NotBlank(message = "Protocol name is required", groups = {OnIndividualSave.class})
    private String protocolName;

    @NotNull(message = "Protocol version is required", groups = {OnIndividualSave.class})
    private Integer protocolVersion;

    @NotNull(message = "Client ID is required", groups = {OnIndividualSave.class})
    private Integer clientId;

    private List<AuditQuestionRequestDTO> auditQuestions;
}