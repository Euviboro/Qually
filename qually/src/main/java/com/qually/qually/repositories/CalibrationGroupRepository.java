package com.qually.qually.repositories;

import com.qually.qually.models.CalibrationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationGroupRepository extends JpaRepository<CalibrationGroup, Long> {

    List<CalibrationGroup> findByRound_RoundId(Long roundId);

    /**
     * Used during round creation to enforce the unique constraint
     * before hitting the database — provides a cleaner error message
     * than a constraint violation exception.
     */
    Optional<CalibrationGroup> findByRound_RoundIdAndInteractionId(
            Long roundId, String interactionId);

    /**
     * All groups for a round with their sessions eagerly loaded.
     * Used by closeAndCompare to fetch everything needed for the
     * comparison in two queries rather than N+1.
     */
    @Query("""
        SELECT g FROM CalibrationGroup g
        LEFT JOIN FETCH g.sessions s
        LEFT JOIN FETCH s.user
        WHERE g.round.roundId = :roundId
    """)
    List<CalibrationGroup> findByRoundIdWithSessions(@Param("roundId") Long roundId);
}