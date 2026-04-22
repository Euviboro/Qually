package com.qually.qually.dto.request;

import com.qually.qually.models.enums.AuditStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Payload for partially updating an existing audit session.
 * All fields are optional — only non-null fields are applied.
 */
@Getter
@Setter
public class AuditSessionUpdateRequestDTO {
    private AuditStatus auditStatus;
    private String comments;
    private LocalDateTime submittedAt;
}
