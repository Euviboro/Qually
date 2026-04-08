package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditedProcessRequestDTO {

    @NotBlank(message = "Process name is required")
    private String auditedProcessName;
}