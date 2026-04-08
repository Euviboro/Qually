package com.qually.qually.dto.request;

import com.qually.qually.models.enums.AuditLogicType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditSessionRequestDTO {

    @NotNull(message = "Protocol ID is required")
    private Integer protocolId;

    @NotNull(message ="Interaction ID is required")
    private String interactionId;

    @NotBlank(message = "Auditor email is required")
    @Email(message = "Must be a valid email")
    private String auditorEmail;

    @NotNull(message = "Audit logic type is required")
    private AuditLogicType auditLogicType;

    private String comments;
}