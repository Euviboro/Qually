package com.qually.qually.repositories;

import com.qually.qually.models.AuditQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditQuestionRepository extends JpaRepository<AuditQuestion, Integer> {
    List<AuditQuestion> findByAuditProtocol_ProtocolId(Integer protocolId);
}