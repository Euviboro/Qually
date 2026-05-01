package com.qually.qually.dto.request;

import com.qually.qually.groups.OnIndividualSave;
import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.ProtocolStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for creating or updating an audit protocol.
 *
 * <p>{@code auditLogicType} is a required field — the scoring strategy is a
 * protocol-level decision that applies to every session conducted against it.</p>
 */
@Getter
@Setter
public class AuditProtocolRequestDTO {

    @NotBlank(message = "Protocol name is required", groups = {OnIndividualSave.class})
    private String protocolName;

    /**
     * Short uppercase abbreviation for calibration round name generation.
     * Optional — can be set later. Must be 2–10 uppercase letters when provided.
     */
    @Size(min = 2, max = 10, message = "Abbreviation must be between 2 and 10 characters")
    @Pattern(regexp = "^[A-Z0-9]*$", message = "Abbreviation must contain only uppercase letters and digits")
    private String protocolAbbreviation;

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