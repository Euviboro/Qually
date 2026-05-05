package com.qually.qually.services;

import com.qually.qually.dto.request.AuditProtocolRequestDTO;
import com.qually.qually.dto.request.SubattributeRequestDTO;
import com.qually.qually.dto.response.AuditProtocolResponseDTO;
import com.qually.qually.mappers.AuditProtocolMapper;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.AuditQuestion;
import com.qually.qually.models.Client;
import com.qually.qually.models.Subattribute;
import com.qually.qually.models.enums.AuditLogicType;
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

        // Validate accountability structure before persisting when creating as FINALIZED
        if (ProtocolStatus.FINALIZED.equals(dto.getProtocolStatus())
                && AuditLogicType.ACCOUNTABILITY.equals(dto.getAuditLogicType())) {
            validateAccountabilityStructureFromDTO(dto);
        }

        AuditProtocol saved = auditProtocolRepository.save(
                auditProtocolMapper.toEntity(dto, client));

        log.info("Protocol {} '{}' created for client {} — logicType {} status {}",
                saved.getProtocolId(), saved.getProtocolName(),
                client.getClientId(), saved.getAuditLogicType(), saved.getProtocolStatus());

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

        // Validate accountability structure when finalizing an existing ACCOUNTABILITY protocol
        if (AuditLogicType.ACCOUNTABILITY.equals(protocol.getAuditLogicType())) {
            validateAccountabilityStructureFromEntity(protocol);
        }

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

    // ── Validation helpers ────────────────────────────────────

    /**
     * Validates that every question in an ACCOUNTABILITY protocol has exactly one
     * subattribute flagged as the accountability selector.
     *
     * <p>Used when creating a protocol directly as FINALIZED (new-protocol flow).</p>
     *
     * @throws IllegalStateException listing all offending question indices (1-based).
     */
    private void validateAccountabilityStructureFromDTO(AuditProtocolRequestDTO dto) {
        if (dto.getAuditQuestions() == null || dto.getAuditQuestions().isEmpty()) {
            throw new IllegalStateException(
                    "An Accountability protocol must have at least one question.");
        }

        List<Integer> offenders = new java.util.ArrayList<>();
        for (int i = 0; i < dto.getAuditQuestions().size(); i++) {
            var q = dto.getAuditQuestions().get(i);
            long count = q.getSubattributes() == null ? 0L :
                    q.getSubattributes().stream()
                            .filter(SubattributeRequestDTO::isAccountabilitySubattribute)
                            .count();
            if (count != 1) offenders.add(i + 1);
        }

        if (!offenders.isEmpty()) {
            throw new IllegalStateException(buildErrorMessage(offenders));
        }
    }

    /**
     * Validates the same rule against an already-persisted entity.
     *
     * <p>Used when finalizing a DRAFT protocol via the ShowProtocol page.</p>
     *
     * @throws IllegalStateException listing all offending question indices (1-based).
     */
    private void validateAccountabilityStructureFromEntity(AuditProtocol protocol) {
        List<AuditQuestion> questions = protocol.getAuditQuestions();

        if (questions == null || questions.isEmpty()) {
            throw new IllegalStateException(
                    "An Accountability protocol must have at least one question.");
        }

        List<Integer> offenders = new java.util.ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            long count = questions.get(i).getSubattributes() == null ? 0L :
                    questions.get(i).getSubattributes().stream()
                            .filter(Subattribute::isAccountability)
                            .count();
            if (count != 1) offenders.add(i + 1);
        }

        if (!offenders.isEmpty()) {
            throw new IllegalStateException(buildErrorMessage(offenders));
        }
    }

    private String buildErrorMessage(List<Integer> offendingQuestionNumbers) {
        return ("Cannot finalize an Accountability protocol: every question must have " +
                "exactly one accountability subattribute. " +
                "Question%s missing it: %s.")
                .formatted(
                        offendingQuestionNumbers.size() > 1 ? "s" : "",
                        offendingQuestionNumbers.toString()
                                .replace("[", "").replace("]", "")
                );
    }
}