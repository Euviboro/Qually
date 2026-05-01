package com.qually.qually.repositories;

import com.qually.qually.models.CalibrationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationParticipantRepository
        extends JpaRepository<CalibrationParticipant, Long> {

    List<CalibrationParticipant> findByRound_RoundId(Long roundId);

    /**
     * Checks whether a user is enrolled in a round.
     * Used by submitAnswer to reject answers from non-participants.
     */
    Optional<CalibrationParticipant> findByRound_RoundIdAndUser_UserId(
            Long roundId, Integer userId);

    /**
     * Finds the expert for a round.
     * There is exactly one expert per round — enforced by the partial
     * unique index {@code uq_round_expert} in the database.
     */
    Optional<CalibrationParticipant> findByRound_RoundIdAndIsExpertTrue(Long roundId);
}