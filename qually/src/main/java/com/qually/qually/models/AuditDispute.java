package com.qually.qually.models;

import com.qually.qually.models.enums.ResolutionOutcome;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a formal dispute raised against a single {@link AuditResponse}.
 *
 * <p>One dispute per response maximum ({@code UNIQUE} constraint on
 * {@code response_id}). Created only when a Team Leader (or above in OPERATIONS)
 * formally escalates a flagged response. Resolved exclusively by QA.</p>
 *
 * <p>{@code newAnswer} is only populated when {@code resolutionOutcome = MODIFIED}.
 * Score recalculation reads this field to compute the post-dispute scores.</p>
 */
@Entity
@Table(name = "audit_disputes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispute_id")
    private Integer disputeId;

    /** The response being disputed. One dispute per response. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false, unique = true)
    private AuditResponse response;

    /** OPERATIONS user (Team Leader or above) who formally raised the dispute. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by", nullable = false)
    private User raisedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reason_id", nullable = false)
    private DisputeReason reason;

    @Column(name = "dispute_comment", columnDefinition = "TEXT")
    private String disputeComment;

    @Column(name = "raised_at", nullable = false)
    private LocalDateTime raisedAt;

    /** QA user (auditor's manager or above) who resolved the dispute. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolution_date")
    private LocalDateTime resolutionDate;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_outcome", length = 20)
    private ResolutionOutcome resolutionOutcome;

    /**
     * The answer QA selected when overriding the original response.
     * Only set when {@code resolutionOutcome = MODIFIED}.
     * Valid values: {@code "YES"}, {@code "NO"}, {@code "N/A"}.
     */
    @Column(name = "new_answer", length = 10)
    private String newAnswer;
}
