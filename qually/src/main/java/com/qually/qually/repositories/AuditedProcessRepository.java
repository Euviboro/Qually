package com.qually.qually.repositories;

import com.qually.qually.models.AuditedProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditedProcessRepository extends JpaRepository<AuditedProcess, Integer> {
    Optional<AuditedProcess> findByAuditedProcessName(String auditedProcessName);
}