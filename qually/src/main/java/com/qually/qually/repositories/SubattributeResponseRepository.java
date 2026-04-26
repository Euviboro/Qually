package com.qually.qually.repositories;

import com.qually.qually.models.SubattributeResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubattributeResponseRepository extends JpaRepository<SubattributeResponse, Long> {

    List<SubattributeResponse> findByAuditResponse_AuditResponseId(Long auditResponseId);

    /**
     * Deletes all subattribute selections for a given audit response.
     * Called before re-saving to avoid duplicates when a DRAFT is updated.
     */
    void deleteByAuditResponse_AuditResponseId(Long auditResponseId);

    /**
     * Deletes all subattribute selections for a given session.
     * Must be called before {@code AuditResponseRepository.deleteByAuditSession_SessionId}
     * to satisfy the FK constraint.
     */
    void deleteByAuditResponse_AuditSession_SessionId(Long sessionId);

    /**
     * Bulk-fetches all subattribute selections for a session in one query,
     * eagerly loading the selected option and its parent subattribute so the
     * resume service can group them by response without additional lazy loads.
     *
     * Used by {@link com.qually.qually.services.AuditSessionService#getSessionForResume}.
     */
    @Query("""
        SELECT sr FROM SubattributeResponse sr
        JOIN FETCH sr.selectedOption opt
        JOIN FETCH opt.subattribute
        WHERE sr.auditResponse.auditSession.sessionId = :sessionId
    """)
    List<SubattributeResponse> findBySessionIdWithDetails(@Param("sessionId") Long sessionId);
}