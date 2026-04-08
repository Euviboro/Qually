package com.qually.qually.repositories;

import com.qually.qually.models.AuditResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditResponseRepository extends JpaRepository<AuditResponse, Long> {
    List<AuditResponse> findByAuditSession_SessionId(Long sessionId);
}
