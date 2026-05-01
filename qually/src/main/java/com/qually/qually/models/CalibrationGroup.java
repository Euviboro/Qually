package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one interaction ID within a {@link CalibrationRound}.
 *
 * <p>Each group is one call, chat, or interaction that all enrolled
 * participants review and answer the round's question about. A round
 * typically has up to 3 groups.</p>
 *
 * <p>{@code isCalibrated} reflects whether all participants matched
 * the expert's answer for this specific interaction. The overall round
 * result is derived from all its groups.</p>
 */
@Entity
@Table(
        name = "calibration_groups",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_round_interaction",
                columnNames = { "round_id", "interaction_id" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalibrationGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private CalibrationRound round;

    /**
     * The ID of the interaction (call, chat, etc.) being calibrated.
     * This is independent from audit session interaction IDs —
     * calibration interactions are not audits.
     */
    @Column(name = "interaction_id", nullable = false, length = 100)
    private String interactionId;

    /**
     * Calibration result for this interaction.
     * {@code null} — round still open.
     * {@code true} — all participants matched the expert.
     * {@code false} — at least one participant did not match.
     */
    @Column(name = "is_calibrated")
    private Boolean isCalibrated;

    /** All answers submitted for this interaction. */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CalibrationSession> sessions = new ArrayList<>();
}