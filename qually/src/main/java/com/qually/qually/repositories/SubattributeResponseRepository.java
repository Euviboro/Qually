package com.qually.qually.repositories;

import com.qually.qually.models.SubattributeResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubattributeResponseRepository extends JpaRepository<SubattributeResponse, Long> {

    List<SubattributeResponse> findByAuditResponse_AuditResponseId(Long auditResponseId);

    /**
     * Deletes all subattribute selections for a given audit response.
     * Called before re-saving to avoid accumulating duplicates when a
     * DRAFT session is updated and re-submitted.
     */
    void deleteByAuditResponse_AuditResponseId(Long auditResponseId);

    /**
     * Deletes all subattribute selections for a given session.
     * Used alongside {@code AuditResponseRepository.deleteByAuditSession_SessionId}
     * to fully clear a session before re-saving responses.
     */
    void deleteByAuditResponse_AuditSession_SessionId(Long sessionId);
}
