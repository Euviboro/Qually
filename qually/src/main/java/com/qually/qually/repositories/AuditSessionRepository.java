package com.qually.qually.repositories;

import com.qually.qually.models.AuditSession;
import com.qually.qually.models.enums.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link AuditSession}.
 *
 * <p>Item 16 — two unused methods removed:</p>
 * <ul>
 *   <li>{@code findByStatusAndClientIds} — defined but never called.
 *       The dispute inbox uses {@code findByAuditStatus} + in-memory client
 *       filtering in the service layer.</li>
 *   <li>{@code findByAuditProtocol_ProtocolId} — defined but never called.
 *       The results page filters by protocol ID in memory after the paginated
 *       visibility fetch.</li>
 * </ul>
 *
 * <p>All methods used by {@link com.qually.qually.services.ResultsService}
 * use {@code JOIN FETCH} to eagerly load associations and avoid N+1 queries.
 * Separate {@code countQuery} clauses omit the fetches so the COUNT(*) runs
 * cheaply.</p>
 */
@Repository
public interface AuditSessionRepository extends JpaRepository<AuditSession, Long> {

    List<AuditSession> findByAuditStatus(AuditStatus status);

    List<AuditSession> findByAuditor_UserId(Integer userId);

    List<AuditSession> findByMemberAuditedUser_UserId(Integer userId);

    // ── JOIN FETCH queries for ResultsService ─────────────────

    @Query(
        value = """
            SELECT s FROM AuditSession s
            JOIN FETCH s.auditProtocol p
            JOIN FETCH p.client
            LEFT JOIN FETCH s.auditor
            LEFT JOIN FETCH s.memberAuditedUser
            LEFT JOIN FETCH s.lob
        """,
        countQuery = "SELECT COUNT(s) FROM AuditSession s"
    )
    Page<AuditSession> findAllWithDetails(Pageable pageable);

    @Query(
        value = """
            SELECT s FROM AuditSession s
            JOIN FETCH s.auditProtocol p
            JOIN FETCH p.client c
            LEFT JOIN FETCH s.auditor
            LEFT JOIN FETCH s.memberAuditedUser
            LEFT JOIN FETCH s.lob
            WHERE c.clientId IN :clientIds
        """,
        countQuery = """
            SELECT COUNT(s) FROM AuditSession s
            WHERE s.auditProtocol.client.clientId IN :clientIds
        """
    )
    Page<AuditSession> findByClientIdsWithDetails(
            @Param("clientIds") List<Integer> clientIds, Pageable pageable);

    @Query(
        value = """
            SELECT s FROM AuditSession s
            JOIN FETCH s.auditProtocol p
            JOIN FETCH p.client
            LEFT JOIN FETCH s.auditor
            LEFT JOIN FETCH s.memberAuditedUser u
            LEFT JOIN FETCH s.lob
            WHERE u.userId = :userId
        """,
        countQuery = """
            SELECT COUNT(s) FROM AuditSession s
            WHERE s.memberAuditedUser.userId = :userId
        """
    )
    Page<AuditSession> findByMemberAuditedWithDetails(
            @Param("userId") Integer userId, Pageable pageable);
}
