package com.qually.qually.dto.request;

import com.qually.qually.groups.OnIndividualSave;
import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.ProtocolStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for creating or updating an audit protocol.
 *
 * <p>{@code auditLogicType} added as a required field, matching the
 * {@code audit_logic_type NOT NULL} column in {@code audit_protocols}.
 * Previously this lived on the session DTO — it has been moved here because
 * the scoring strategy is a protocol-level decision that applies to every
 * session conducted against it.</p>
 */
@Getter
@Setter
public class AuditProtocolRequestDTO {

    @NotBlank(message = "Protocol name is required", groups = {OnIndividualSave.class})
    private String protocolName;

    @NotNull(message = "Protocol version is required", groups = {OnIndividualSave.class})
    private Integer protocolVersion;

    private ProtocolStatus protocolStatus;

    @NotNull(message = "Client ID is required", groups = {OnIndividualSave.class})
    private Integer clientId;

    /** Scoring strategy applied to all sessions that use this protocol. */
    @NotNull(message = "Audit logic type is required", groups = {OnIndividualSave.class})
    private AuditLogicType auditLogicType;

    private List<AuditQuestionRequestDTO> auditQuestions;
}
