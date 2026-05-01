package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records one participant's answer for one interaction within a calibration round.
 *
 * <p>One row per user per {@link CalibrationGroup}. A participant who is enrolled
 * in a round with 3 interaction IDs will have 3 {@code CalibrationSession} records
 * once they have answered all of them.</p>
 *
 * <p>Answers are immutable — no update is allowed after submission. The unique
 * constraint {@code uq_group_participant} enforces this at the database level.</p>
 *
 * <p>{@code isCalibrated} is set when the round is closed and compared:
 * {@code true} if this participant's answer matched the expert's answer for
 * this interaction, {@code false} if it did not. The expert's own session
 * is always set to {@code true}.</p>
 */
@Entity
@Table(
        name = "calibration_sessions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_group_participant",
                columnNames = { "group_id", "user_id" }
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalibrationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calibration_session_id")
    private Long calibrationSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private CalibrationGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The participant's answer for this interaction.
     * Valid values: {@code "YES"}, {@code "NO"}, {@code "N/A"}.
     * Validated against {@link com.qually.qually.models.enums.AuditAnswerType}.
     */
    @Column(name = "calibration_answer", nullable = false, length = 10)
    private String calibrationAnswer;

    /**
     * Whether this participant's answer matched the expert's for this interaction.
     * {@code null} — round not yet closed and compared.
     * {@code true} — answer matched the expert.
     * {@code false} — answer did not match the expert.
     */
    @Column(name = "is_calibrated")
    private Boolean isCalibrated;

    @Column(name = "answered_at", nullable = false)
    @Builder.Default
    private LocalDateTime answeredAt = LocalDateTime.now();
}