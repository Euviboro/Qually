package com.qually.qually.models;

import com.qually.qually.models.enums.ResponseStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a single YES / NO / N/A answer to an audit question within a session.
 *
 * <p><strong>Changes:</strong></p>
 * <ul>
 *   <li>{@code responseStatus} added — tracks the dispute lifecycle for this
 *       individual response (ANSWERED → FLAGGED → DISPUTED → RESOLVED).</li>
 *   <li>{@code dispute} relationship added — the formal dispute entry, if one
 *       was raised against this response. {@code null} for most responses.</li>
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
     * Never overwritten — dispute overrides are stored in {@link AuditDispute#getNewAnswer()}.
     */
    @Column(name = "question_answer", nullable = false, length = 100)
    private String questionAnswer;

    /**
     * Dispute lifecycle status for this response.
     * Defaults to ANSWERED when the response is first saved.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 20)
    @Builder.Default
    private ResponseStatus responseStatus = ResponseStatus.ANSWERED;

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
