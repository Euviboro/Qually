package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Enrolls a user as a participant in a {@link CalibrationRound}.
 *
 * <p>One row per user per round. When a participant is marked as the expert
 * ({@code isExpert = true}), their answer becomes the reference against which
 * all other participants are compared when the round is closed.</p>
 *
 * <p>The database enforces exactly one expert per round via a partial unique
 * index on {@code (round_id) WHERE is_expert = TRUE}. The application also
 * validates this in {@link com.qually.qually.services.CalibrationService}.</p>
 *
 * <p>The expert's identity is visible to QA managers but hidden from other
 * participants — they see only the reference answer, not whose it is.</p>
 */
@Entity
@Table(
        name = "calibration_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_round_participant",
                columnNames = { "round_id", "user_id" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalibrationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private CalibrationRound round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Whether this participant is the expert for the round.
     * The expert's answer is the reference — all other answers are
     * compared against it when the round closes.
     * Exactly one participant per round may have this set to {@code true}.
     */
    @Column(name = "is_expert", nullable = false)
    @Builder.Default
    private Boolean isExpert = false;
}