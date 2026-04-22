package com.qually.qually.repositories;

import com.qually.qually.models.SubattributeResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttributeResponseRepository extends JpaRepository<SubattributeResponse, Long> {
    List<SubattributeResponse> findByAuditResponse_AuditResponseId(Long auditResponseId);
}