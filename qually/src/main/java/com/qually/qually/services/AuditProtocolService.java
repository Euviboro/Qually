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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditProtocolService {

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

        auditProtocolRepository.findByProtocolNameAndClient_ClientId(dto.getProtocolName(), dto.getClientId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A protocol named '%s' already exists for this client".formatted(dto.getProtocolName()));
                });

        return auditProtocolMapper.toDTO(
                auditProtocolRepository.save(auditProtocolMapper.toEntity(dto, client)));
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

    /**
     * Replaces the mutable fields of an existing protocol.
     * {@code auditLogicType} is also updated when provided.
     */
    @Transactional
    public AuditProtocolResponseDTO updateProtocol(Integer id, AuditProtocolRequestDTO dto) {
        AuditProtocol protocol = auditProtocolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(id)));
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(dto.getClientId())));

        auditProtocolRepository.findByProtocolNameAndClient_ClientId(dto.getProtocolName(), dto.getClientId())
                .filter(existing -> !existing.getProtocolId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A protocol named '%s' already exists for this client".formatted(dto.getProtocolName()));
                });

        protocol.setProtocolName(dto.getProtocolName());
        protocol.setProtocolVersion(dto.getProtocolVersion());
        protocol.setClient(client);
        if (dto.getAuditLogicType() != null) protocol.setAuditLogicType(dto.getAuditLogicType());

        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }

    @Transactional
    public AuditProtocolResponseDTO finalizeProtocol(Integer id) {
        AuditProtocol protocol = auditProtocolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(id)));
        protocol.setProtocolStatus(ProtocolStatus.FINALIZED);
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
        protocol.setProtocolName(newName);
        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }
}
