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
     * eagerly loading the question and any dispute.
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
     * Safe only for DRAFT sessions.
     */
    void deleteByAuditSession_SessionId(Long sessionId);

    // ── Disputes inbox queries ────────────────────────────────

    /**
     * Responses where the given user is the member audited and the response
     * is flagged or in a formal dispute lifecycle state.
     * Used for the Team Member visibility tier.
     */
    @Query("""
        SELECT r FROM AuditResponse r
        JOIN FETCH r.auditSession s
        JOIN FETCH s.auditProtocol p
        JOIN FETCH p.client
        LEFT JOIN FETCH s.lob
        LEFT JOIN FETCH s.memberAuditedUser
        JOIN FETCH r.auditQuestion q
        LEFT JOIN FETCH r.dispute d
        LEFT JOIN FETCH d.reason
        LEFT JOIN FETCH d.raisedBy
        WHERE s.memberAuditedUser.userId = :userId
          AND (r.isFlagged = true
               OR r.responseStatus IN (
                   com.qually.qually.models.enums.ResponseStatus.DISPUTED,
                   com.qually.qually.models.enums.ResponseStatus.RESOLVED))
        ORDER BY s.startedAt DESC
    """)
    List<AuditResponse> findInboxByMemberAudited(@Param("userId") Integer userId);

    /**
     * Responses where the member audited is anywhere in the management chain
     * below the given user, scoped to the user's assigned clients.
     *
     * <p>Previously filtered by {@code manager_id = :managerId} (one level
     * deep). Now accepts a pre-computed list of all subordinate user IDs from
     * the recursive CTE in {@code UserRepository.findAllSubordinateIds}, so
     * visibility extends to the full management chain at any depth.</p>
     *
     * <p>Client scoping ensures an Account Manager at Client A cannot see
     * disputes for team members at Client B even if a subordinate manages
     * that client.</p>
     */
    @Query("""
        SELECT r FROM AuditResponse r
        JOIN FETCH r.auditSession s
        JOIN FETCH s.auditProtocol p
        JOIN FETCH p.client c
        LEFT JOIN FETCH s.lob
        LEFT JOIN FETCH s.memberAuditedUser m
        JOIN FETCH r.auditQuestion q
        LEFT JOIN FETCH r.dispute d
        LEFT JOIN FETCH d.reason
        LEFT JOIN FETCH d.raisedBy
        WHERE m.userId IN :memberIds
          AND c.clientId IN :clientIds
          AND (r.isFlagged = true
               OR r.responseStatus IN (
                   com.qually.qually.models.enums.ResponseStatus.DISPUTED,
                   com.qually.qually.models.enums.ResponseStatus.RESOLVED))
        ORDER BY s.startedAt DESC
    """)
    List<AuditResponse> findInboxByMemberIds(
            @Param("memberIds") List<Integer> memberIds,
            @Param("clientIds") List<Integer> clientIds);

    /**
     * Disputed and resolved responses for sessions audited by any of the
     * given auditor IDs. Used for the QA visibility tier.
     *
     * <p>The caller passes their own ID plus all subordinate QA auditor IDs
     * from the recursive CTE, so a QA Director sees disputes from every
     * session audited anywhere in their management chain.</p>
     */
    @Query("""
        SELECT r FROM AuditResponse r
        JOIN FETCH r.auditSession s
        JOIN FETCH s.auditProtocol p
        JOIN FETCH p.client
        LEFT JOIN FETCH s.lob
        LEFT JOIN FETCH s.memberAuditedUser
        LEFT JOIN FETCH s.auditor
        JOIN FETCH r.auditQuestion q
        LEFT JOIN FETCH r.dispute d
        LEFT JOIN FETCH d.reason
        LEFT JOIN FETCH d.raisedBy
        WHERE s.auditor.userId IN :auditorIds
          AND r.responseStatus IN (
              com.qually.qually.models.enums.ResponseStatus.DISPUTED,
              com.qually.qually.models.enums.ResponseStatus.RESOLVED)
        ORDER BY s.startedAt DESC
    """)
    List<AuditResponse> findInboxByAuditors(
            @Param("auditorIds") List<Integer> auditorIds);
}