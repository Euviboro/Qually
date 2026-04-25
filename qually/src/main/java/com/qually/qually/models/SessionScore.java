package com.qually.qually.models;

import com.qually.qually.models.enums.CopcCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores the COPC score for one category in one audit session.
 *
 * <p>Three rows are inserted per session (CUSTOMER, BUSINESS, COMPLIANCE) when
 * the session is submitted. If a dispute resolves as MODIFIED, three new rows
 * are inserted with {@code isPostDispute = true}, preserving the original scores
 * for audit trail purposes.</p>
 *
 * <p>Score is always 0 or 100 per COPC rules: any unanswered or NO response in
 * a category fails that category. N/A answers are excluded from the calculation.</p>
 */
@Entity
@Table(name = "session_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Integer scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AuditSession auditSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private CopcCategory category;

    /** 0 or 100. */
    @Column(name = "score", nullable = false)
    private Short score;

    /**
     * {@code false} for scores calculated at submission time.
     * {@code true} for scores recalculated after a MODIFIED dispute resolution.
     */
    @Column(name = "is_post_dispute", nullable = false)
    private Boolean isPostDispute;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}
