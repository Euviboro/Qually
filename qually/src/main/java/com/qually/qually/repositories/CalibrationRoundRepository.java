package com.qually.qually.repositories;

import com.qually.qually.models.CalibrationRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationRoundRepository extends JpaRepository<CalibrationRound, Long> {

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

    @Query("""
        SELECT r FROM CalibrationRound r
        JOIN FETCH r.client
        JOIN FETCH r.protocol
        JOIN FETCH r.question
        JOIN FETCH r.createdBy
        ORDER BY r.createdAt DESC
    """)
    List<CalibrationRound> findAllWithDetails();

    @Query("""
    SELECT r FROM CalibrationRound r
    JOIN FETCH r.client
    JOIN FETCH r.protocol
    JOIN FETCH r.question
    JOIN FETCH r.createdBy
    WHERE r.roundId = :roundId
""")
    Optional<CalibrationRound> findByIdWithDetails(@Param("roundId") Long roundId);

    /**
     * Counts existing rounds for the same client + protocol + question category
     * within the same YYMM period. Used by {@code CalibrationService.generateRoundName}
     * to compute the consecutive sequence number (e.g. 001, 002, 003).
     *
     * <p>Uses a native query because JPQL does not support date formatting functions
     * portably. {@code TO_CHAR} is PostgreSQL-specific — adjust if migrating to
     * another DB.</p>
     */
    @Query(value = """
        SELECT COUNT(*) FROM calibration_rounds r
        JOIN audit_questions q ON r.question_id = q.question_id
        WHERE r.client_id    = :clientId
          AND r.protocol_id  = :protocolId
          AND q.category     = :category
          AND TO_CHAR(r.created_at, 'YYMM') = :period
        """, nativeQuery = true)
    long countByClientAndProtocolAndCategoryAndPeriod(
            @Param("clientId")   Integer clientId,
            @Param("protocolId") Integer protocolId,
            @Param("category")   String  category,
            @Param("period")     String  period);
}