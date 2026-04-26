package com.qually.qually.models;

import com.qually.qually.models.enums.ResponseStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a single YES / NO / N/A answer to an audit question within a session.
 *
 * <p><strong>Changes:</strong></p>
 * <ul>
 *   <li>{@code isFlagged} added — a boolean flag set by OPERATIONS users to
 *       informally mark a response for Team Leader review. This is separate from
 *       the formal dispute lifecycle tracked by {@code responseStatus}.
 *       When a TL formally raises a dispute, {@code isFlagged} is cleared and
 *       {@code responseStatus} moves to {@code DISPUTED}.</li>
 *   <li>{@code responseStatus} now only tracks the formal dispute lifecycle:
 *       {@code ANSWERED → DISPUTED → RESOLVED}. The {@code FLAGGED} value has
 *       been removed — flagging is now represented by {@code isFlagged}.</li>
 * </ul>
 */
@Entity
@Table(name = "audit_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_response_id")
    private Long auditResponseId;

    /**
     * Original answer recorded by the auditor.
     * Valid values: {@code "YES"}, {@code "NO"}, {@code "N/A"}.
     * Never overwritten — dispute overrides are stored in
     * {@link AuditDispute#getNewAnswer()}.
     */
    @Column(name = "question_answer", nullable = false, length = 100)
    private String questionAnswer;

    /**
     * Formal dispute lifecycle status.
     * Defaults to {@link ResponseStatus#ANSWERED} when first saved.
     * Does not include FLAGGED — see {@link #isFlagged}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 20)
    @Builder.Default
    private ResponseStatus responseStatus = ResponseStatus.ANSWERED;

    /**
     * Whether this response has been informally flagged by an OPERATIONS user
     * for Team Leader review. This is a temporary, internal state — it has no
     * QA-facing meaning and is not part of the formal dispute audit trail.
     *
     * <p>Set to {@code true} by {@code DisputeService.flagResponse}.
     * Cleared to {@code false} when the TL raises a formal dispute (via
     * {@code raiseDispute}) or when the flag is rejected (via
     * {@code unflagResponse}).</p>
     */
    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    private Boolean isFlagged = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AuditSession auditSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AuditQuestion auditQuestion;

    /**
     * The formal dispute raised against this response, if any.
     * {@code null} when no dispute has been raised.
     * Loaded lazily — access within a transaction.
     */
    @OneToOne(mappedBy = "response", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AuditDispute dispute;
}