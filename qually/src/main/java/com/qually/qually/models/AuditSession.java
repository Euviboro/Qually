package com.qually.qually.models;

import com.qually.qually.models.enums.AuditStatus;
import com.qually.qually.models.enums.ResolutionOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a single audit session conducted against a protocol.
 *
 * <p><strong>Schema changes in this version:</strong></p>
 * <ul>
 *   <li>{@code memberAudited} (VARCHAR) replaced by {@code memberAuditedUser}
 *       — a FK to {@link User}. The person being audited must be a registered
 *       user with an auditable role (Team Member, Supervisor, Team Leader).</li>
 *   <li>{@code lob} added — FK to {@link Lob}. Selected by the auditor on
 *       the Log Session page, filtered to the protocol's client.</li>
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

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    private AuditProtocol auditProtocol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_user_id")
    private User auditor;

    /**
     * The user whose work is being audited.
     * Must have an auditable role (Team Member, Supervisor, or Team Leader).
     * Cannot be the same user as {@code auditor}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_audited_user_id")
    private User memberAuditedUser;

    /**
     * Line of Business this session belongs to.
     * Filtered to the protocol's client at selection time.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lob_id")
    private Lob lob;

    @Column(name = "started_at", insertable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_outcome", length = 100)
    private ResolutionOutcome resolutionOutcome;
}
