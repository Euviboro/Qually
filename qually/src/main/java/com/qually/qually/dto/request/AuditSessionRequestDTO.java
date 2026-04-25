package com.qually.qually.dto.request;

import com.qually.qually.models.enums.AuditStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditSessionRequestDTO {

    @NotNull(message = "Protocol ID is required")
    private Integer protocolId;

    @NotBlank(message = "Interaction ID is required")
    private String interactionId;

    @NotNull(message = "Auditor user ID is required")
    private Integer auditorUserId;

    /**
     * FK to {@code users.user_id}. Must be an active user with an auditable
     * role (Team Member, Supervisor, or Team Leader) and must differ from
     * {@code auditorUserId}.
     */
    @NotNull(message = "Member audited is required")
    private Integer memberAuditedUserId;

    /**
     * FK to {@code lobs.lob_id}. Must belong to the protocol's client.
     */
    @NotNull(message = "LOB is required")
    private Integer lobId;

    private String comments;

    /** Defaults to {@code DRAFT} when null. */
    private AuditStatus auditStatus;
}
