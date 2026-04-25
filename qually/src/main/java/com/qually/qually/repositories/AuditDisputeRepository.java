package com.qually.qually.repositories;

import com.qually.qually.models.AuditDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditDisputeRepository extends JpaRepository<AuditDispute, Integer> {

    Optional<AuditDispute> findByResponse_AuditResponseId(Long responseId);

    /**
     * Finds all disputes for a given session by joining through audit_responses.
     * Used to check whether all disputes for a session are resolved.
     */
    @Query("""
        SELECT d FROM AuditDispute d
        WHERE d.response.auditSession.sessionId = :sessionId
    """)
    List<AuditDispute> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Returns true when at least one unresolved dispute exists for the session.
     * Used by the resolution service to determine if the session can move to RESOLVED.
     */
    @Query("""
        SELECT COUNT(d) > 0 FROM AuditDispute d
        WHERE d.response.auditSession.sessionId = :sessionId
          AND d.resolutionOutcome IS NULL
    """)
    boolean hasUnresolvedDisputes(@Param("sessionId") Long sessionId);
}
