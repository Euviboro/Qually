package com.qually.qually.repositories;

import com.qually.qually.models.CalibrationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationSessionRepository
        extends JpaRepository<CalibrationSession, Long> {

    /**
     * All sessions for every group in a round.
     * Used by closeAndCompare alongside
     * {@link CalibrationGroupRepository#findByRoundIdWithSessions}
     * to avoid N+1 when processing results.
     */
    @Query("""
        SELECT s FROM CalibrationSession s
        JOIN FETCH s.user
        WHERE s.group.round.roundId = :roundId
    """)
    List<CalibrationSession> findByRoundId(@Param("roundId") Long roundId);

    /**
     * Checks whether a user has already submitted an answer for a
     * specific group. Used to enforce the no-update rule.
     */
    Optional<CalibrationSession> findByGroup_GroupIdAndUser_UserId(
            Long groupId, Integer userId);

    /**
     * All sessions submitted by a user in a round — one per interaction.
     * Used to determine how many interactions a participant has answered
     * and to build their personal result view.
     */
    @Query("""
        SELECT s FROM CalibrationSession s
        JOIN FETCH s.group g
        WHERE g.round.roundId = :roundId
          AND s.user.userId   = :userId
    """)
    List<CalibrationSession> findByRoundIdAndUserId(
            @Param("roundId") Long roundId,
            @Param("userId")  Integer userId);
}