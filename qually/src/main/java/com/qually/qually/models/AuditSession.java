package com.qually.qually.models;

import com.qually.qually.models.enums.AuditStatus;
import com.qually.qually.models.enums.ResolutionOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a single audit session conducted against a protocol.
 *
 * <p><strong>Schema alignment changes:</strong></p>
 * <ul>
 *   <li>{@code auditor} FK now joins on {@code auditor_user_id → users.user_id}
 *       (integer PK). Previously used {@code auditor_email → users.user_email}.</li>
 *   <li>{@code memberAudited} added — the name/ID of the person being audited.
 *       {@code NOT NULL} in the DB.</li>
 *   <li>{@code auditLogicType} removed — it belongs on {@link AuditProtocol},
 *       not the session. The {@code audit_sessions} table has no such column.</li>
 *   <li>{@code resolutionOutcome} column name fixed from the erroneous
 *       {@code "resolutionOutcome"} to the correct snake-case
 *       {@code "resolution_outcome"}.</li>
 * </ul>
 */
@Entity
@Table(name = "audit_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "interaction_id", nullable = false, length = 100)
    private String interactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status", nullable = false, length = 100)
    private AuditStatus auditStatus;

    @Column(name = "comments", columnDefinition = "MEDIUMTEXT")
    private String comments;

    /** Name or identifier of the person whose work is being audited. */
    @Column(name = "member_audited", nullable = false, length = 100)
    private String memberAudited;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    private AuditProtocol auditProtocol;

    /**
     * User conducting the audit.
     * Joined on {@code auditor_user_id → users.user_id} (integer PK).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_user_id")
    private User auditor;

    /** Set by the DB {@code DEFAULT current_timestamp()} — not insertable or updatable by JPA. */
    @Column(name = "started_at", insertable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /** Column name corrected from {@code resolutionOutcome} to {@code resolution_outcome}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_outcome", length = 100)
    private ResolutionOutcome resolutionOutcome;
}
