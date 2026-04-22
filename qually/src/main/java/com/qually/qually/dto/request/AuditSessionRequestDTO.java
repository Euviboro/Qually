package com.qually.qually.dto.request;

import com.qually.qually.models.enums.AuditStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating a new audit session.
 *
 * <p><strong>Schema alignment changes:</strong></p>
 * <ul>
 *   <li>{@code auditorEmail} → {@code auditorUserId} (Integer): the auditor FK
 *       now references {@code users.user_id}, not {@code users.user_email}.</li>
 *   <li>{@code memberAudited} added (NOT NULL in DB): the name/ID of the person
 *       being audited.</li>
 *   <li>{@code auditLogicType} removed: it now lives on the protocol, not the
 *       session.</li>
 *   <li>{@code auditStatus} is optional: when omitted the service defaults to
 *       {@link AuditStatus#DRAFT}. Pass {@link AuditStatus#COMPLETED} to
 *       create and submit in one step.</li>
 * </ul>
 */
@Getter
@Setter
public class AuditSessionRequestDTO {

    @NotNull(message = "Protocol ID is required")
    private Integer protocolId;

    @NotBlank(message = "Interaction ID is required")
    private String interactionId;

    @NotNull(message = "Auditor user ID is required")
    private Integer auditorUserId;

    @NotBlank(message = "Member audited is required")
    private String memberAudited;

    private String comments;

    /**
     * Defaults to {@link AuditStatus#DRAFT} when null.
     * Only {@code DRAFT} and {@code COMPLETED} are valid at creation time.
     */
    private AuditStatus auditStatus;
}
