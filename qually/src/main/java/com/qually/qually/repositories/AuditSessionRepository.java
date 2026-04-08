package com.qually.qually.repositories;

import com.qually.qually.models.AuditSession;
import com.qually.qually.models.Lob;
import com.qually.qually.models.enums.AuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditSessionRepository extends JpaRepository<AuditSession, Long> {
    List<AuditSession> findByAuditStatus(AuditStatus status);

    List<AuditSession> findByAuditor_UserEmail(String email);

    List<AuditSession> findByInteractionId(String interactionId);
}