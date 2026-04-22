package com.qually.qually.repositories;

import com.qually.qually.models.AuditSession;
import com.qually.qually.models.enums.AuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link AuditSession}.
 *
 * <p>{@code findByAuditor_UserEmail} has been replaced by
 * {@code findByAuditor_UserId} because the {@code auditor} FK now joins on
 * {@code users.user_id} (integer PK) rather than {@code users.user_email}.</p>
 */
@Repository
public interface AuditSessionRepository extends JpaRepository<AuditSession, Long> {
    List<AuditSession> findByAuditStatus(AuditStatus status);
    List<AuditSession> findByAuditor_UserId(Integer userId);
    List<AuditSession> findByInteractionId(String interactionId);
}
