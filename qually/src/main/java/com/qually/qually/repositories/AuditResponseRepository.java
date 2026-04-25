package com.qually.qually.repositories;

import com.qually.qually.models.AuditResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditResponseRepository extends JpaRepository<AuditResponse, Long> {

    List<AuditResponse> findByAuditSession_SessionId(Long sessionId);

    /**
     * Bulk-fetches all responses for a set of session IDs in a single query,
     * eagerly loading the question and any dispute so the caller can compute
     * effective answers without triggering additional lazy loads.
     *
     * Used by {@link com.qually.qually.services.ResultsService} to eliminate
     * the N+1 that occurred when responses were fetched per session in a loop.
     */
    @Query("""
        SELECT r FROM AuditResponse r
        JOIN FETCH r.auditQuestion q
        LEFT JOIN FETCH r.dispute d
        LEFT JOIN FETCH d.reason
        WHERE r.auditSession.sessionId IN :sessionIds
    """)
    List<AuditResponse> findBySessionIdInWithDetails(
            @Param("sessionIds") List<Long> sessionIds);

    /**
     * Deletes all responses for a session before re-saving updated answers.
     * Safe to call only on DRAFT sessions — disputes can only be raised on
     * COMPLETED sessions so no dispute FK constraint will be violated.
     */
    void deleteByAuditSession_SessionId(Long sessionId);
}
