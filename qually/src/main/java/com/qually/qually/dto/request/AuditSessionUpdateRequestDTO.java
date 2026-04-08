package com.qually.qually.dto.request;

import com.qually.qually.models.enums.AuditStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AuditSessionUpdateRequestDTO {

    private AuditStatus auditStatus;
    private String comments;
    private LocalDateTime submittedAt;
}