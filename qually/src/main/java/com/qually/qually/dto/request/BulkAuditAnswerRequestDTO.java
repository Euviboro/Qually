package com.qually.qually.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkAuditAnswerRequestDTO {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotEmpty(message = "At least one response is required")
    @Valid
    private List<AuditResponseItemDTO> responses;
}