package com.qually.qually.repositories;

import com.qually.qually.models.CalibrationRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CalibrationRoundRepository extends JpaRepository<CalibrationRound, Long> {

    /**
     * Rounds where the given user is enrolled as a participant.
     * Used for the QA Specialist visibility tier — they see only
     * rounds they are explicitly enrolled in.
     */
    @Query("""
        SELECT DISTINCT r FROM CalibrationRound r
        JOIN FETCH r.client
        JOIN FETCH r.protocol
        JOIN FETCH r.question
        JOIN FETCH r.createdBy
        JOIN r.participants p
        WHERE p.user.userId = :userId
        ORDER BY r.createdAt DESC
    """)
    List<CalibrationRound> findByParticipantUserId(@Param("userId") Integer userId);

    /**
     * Rounds for a set of clients — used for QA manager visibility
     * tier scoped to their management chain's clients.
     */
    @Query("""
        SELECT r FROM CalibrationRound r
        JOIN FETCH r.client
        JOIN FETCH r.protocol
        JOIN FETCH r.question
        JOIN FETCH r.createdBy
        WHERE r.client.clientId IN :clientIds
        ORDER BY r.createdAt DESC
    """)
    List<CalibrationRound> findByClientIds(@Param("clientIds") List<Integer> clientIds);

    /**
     * All rounds with associations eagerly loaded — used for QA Director
     * level visibility (all rounds across all clients).
     */
    @Query("""
        SELECT r FROM CalibrationRound r
        JOIN FETCH r.client
        JOIN FETCH r.protocol
        JOIN FETCH r.question
        JOIN FETCH r.createdBy
        ORDER BY r.createdAt DESC
    """)
    List<CalibrationRound> findAllWithDetails();

    /**
     * Full round with groups, participants, and their users eagerly loaded.
     * Used by getRoundDetail to avoid N+1 when building the response.
     */
    @Query("""
        SELECT r FROM CalibrationRound r
        JOIN FETCH r.client
        JOIN FETCH r.protocol
        JOIN FETCH r.question
        JOIN FETCH r.createdBy
        LEFT JOIN FETCH r.groups g
        LEFT JOIN FETCH r.participants p
        LEFT JOIN FETCH p.user
        WHERE r.roundId = :roundId
    """)
    java.util.Optional<CalibrationRound> findByIdWithDetails(@Param("roundId") Long roundId);
}