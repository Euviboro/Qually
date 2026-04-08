package com.qually.qually.repositories;

import com.qually.qually.models.AuditProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditProtocolRepository extends JpaRepository<AuditProtocol, Integer> {
    List<AuditProtocol> findByClient_ClientId(Integer clientId);
    Optional<AuditProtocol> findByProtocolNameAndClient_ClientId(String protocolName, Integer clientId);
}