package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-level container for a calibration period.
 *
 * <p>A round groups multiple interactions ({@link CalibrationGroup}) that
 * all participants review against the same question. The round is created
 * by a QA Specialist and closed manually by their QA manager, at which
 * point answers are compared against the expert's and calibration results
 * are recorded.</p>
 *
 * <p>Visibility: participants see only rounds they are enrolled in via
 * {@link CalibrationParticipant}. QA managers see all rounds in their
 * management chain.</p>
 */
@Entity
@Table(name = "calibration_rounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalibrationRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_id")
    private Long roundId;

    /** Human-readable label e.g. "April 2026 Calibration" */
    @Column(name = "round_name", nullable = false, length = 200)
    private String roundName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    private AuditProtocol protocol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AuditQuestion question;

    /**
     * Whether the round is still accepting answers.
     * Set to {@code false} by a QA manager when closing the round.
     * No more answers can be submitted once closed.
     */
    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private Boolean isOpen = true;

    /**
     * Overall calibration result.
     * {@code null} — round is still open.
     * {@code true} — all participants matched the expert on all interactions.
     * {@code false} — at least one participant failed on at least one interaction.
     * Populated when the round is closed and compared.
     */
    @Column(name = "is_calibrated")
    private Boolean isCalibrated;

    /** The QA Specialist who created this round. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** The interaction IDs (calls/chats) assigned for this round. */
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalibrationGroup> groups = new ArrayList<>();

    /** The users enrolled to participate in this round. */
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalibrationParticipant> participants = new ArrayList<>();
}