package com.qually.qually.models;

import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.ProtocolStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an audit protocol.
 *
 * <p><strong>Schema alignment:</strong> {@code auditLogicType} has been moved
 * here from {@code AuditSession}, matching the DB where the column lives on
 * {@code audit_protocols}. The scoring strategy is a protocol-level concern —
 * all sessions that use this protocol apply the same logic.</p>
 */
@Entity
@Table(name = "audit_protocols")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "protocol_id")
    private Integer protocolId;

    @Column(name = "protocol_name", nullable = false, length = 100)
    private String protocolName;

    /**
     * Short uppercase abbreviation used in auto-generated calibration round names.
     * Example: "COPC", "DSP", "CXQ". Does not need to be unique globally
     * but should be unique within a client's protocols for clarity.
     * Must be set before a calibration round can be created using this protocol.
     */
    @Column(name = "protocol_abbreviation", length = 10)
    private String protocolAbbreviation;

    @Column(name = "protocol_version", nullable = false)
    private Integer protocolVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol_status", nullable = false, columnDefinition = "MEDIUMTEXT")
    private ProtocolStatus protocolStatus;

    /**
     * Scoring strategy for all sessions conducted against this protocol.
     * Stored as a VARCHAR(100) in the DB.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "audit_logic_type", nullable = false, length = 100)
    private AuditLogicType auditLogicType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(mappedBy = "auditProtocol", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditQuestion> auditQuestions = new ArrayList<>();
}