package com.qually.qually.services;

import com.qually.qually.dto.request.AuditProtocolRequestDTO;
import com.qually.qually.dto.response.AuditProtocolResponseDTO;
import com.qually.qually.mappers.AuditProtocolMapper;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.Client;
import com.qually.qually.models.enums.ProtocolStatus;
import com.qually.qually.repositories.AuditProtocolRepository;
import com.qually.qually.repositories.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditProtocolService {

    private static final Logger log = LoggerFactory.getLogger(AuditProtocolService.class);

    private final AuditProtocolRepository auditProtocolRepository;
    private final ClientRepository clientRepository;
    private final AuditProtocolMapper auditProtocolMapper;

    public AuditProtocolService(AuditProtocolRepository auditProtocolRepository,
                                ClientRepository clientRepository,
                                AuditProtocolMapper auditProtocolMapper) {
        this.auditProtocolRepository = auditProtocolRepository;
        this.clientRepository = clientRepository;
        this.auditProtocolMapper = auditProtocolMapper;
    }

    @Transactional
    public AuditProtocolResponseDTO createProtocol(AuditProtocolRequestDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(dto.getClientId())));

        auditProtocolRepository
                .findByProtocolNameAndClient_ClientId(dto.getProtocolName(), dto.getClientId())
                .ifPresent(existing -> {
                    log.warn("Duplicate protocol name '{}' rejected for client {}",
                            dto.getProtocolName(), dto.getClientId());
                    throw new IllegalArgumentException(
                            "A protocol named '%s' already exists for this client"
                            .formatted(dto.getProtocolName()));
                });

        AuditProtocol saved = auditProtocolRepository.save(
                auditProtocolMapper.toEntity(dto, client));

        log.info("Protocol {} '{}' created for client {} — logicType {}",
                saved.getProtocolId(), saved.getProtocolName(),
                client.getClientId(), saved.getAuditLogicType());

        return auditProtocolMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditProtocolResponseDTO> getAllProtocols(Integer clientId) {
        List<AuditProtocol> protocols = (clientId != null)
                ? auditProtocolRepository.findByClient_ClientId(clientId)
                : auditProtocolRepository.findAll();
        return protocols.stream().map(auditProtocolMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public AuditProtocolResponseDTO getProtocolById(Integer id) {
        return auditProtocolRepository.findById(id)
                .map(auditProtocolMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(id)));
    }

    @Transactional
    public AuditProtocolResponseDTO updateProtocol(Integer id, AuditProtocolRequestDTO dto) {
        AuditProtocol protocol = auditProtocolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(id)));
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(dto.getClientId())));

        auditProtocolRepository
                .findByProtocolNameAndClient_ClientId(dto.getProtocolName(), dto.getClientId())
                .filter(existing -> !existing.getProtocolId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A protocol named '%s' already exists for this client"
                            .formatted(dto.getProtocolName()));
                });

        protocol.setProtocolName(dto.getProtocolName());
        protocol.setProtocolVersion(dto.getProtocolVersion());
        protocol.setClient(client);
        if (dto.getAuditLogicType() != null) protocol.setAuditLogicType(dto.getAuditLogicType());

        log.info("Protocol {} updated — name '{}' version {}",
                id, dto.getProtocolName(), dto.getProtocolVersion());

        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }

    @Transactional
    public AuditProtocolResponseDTO finalizeProtocol(Integer id) {
        AuditProtocol protocol = auditProtocolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(id)));
        protocol.setProtocolStatus(ProtocolStatus.FINALIZED);
        log.info("Protocol {} '{}' finalized", id, protocol.getProtocolName());
        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }

    @Transactional
    public AuditProtocolResponseDTO updateProtocolName(Integer id, String newName) {
        AuditProtocol protocol = auditProtocolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(id)));
        if (ProtocolStatus.FINALIZED.equals(protocol.getProtocolStatus())) {
            throw new IllegalStateException("Cannot change the name of a finalized protocol.");
        }
        String oldName = protocol.getProtocolName();
        protocol.setProtocolName(newName);
        log.info("Protocol {} renamed: '{}' → '{}'", id, oldName, newName);
        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }
}
