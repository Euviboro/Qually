package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditedProcessResponseDTO {
        private Integer auditedProcessId;
        private String auditedProcessName;
}