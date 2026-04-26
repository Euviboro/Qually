package com.qually.qually.services;

import com.qually.qually.dto.request.AuditSessionRequestDTO;
import com.qually.qually.dto.request.AuditSessionUpdateRequestDTO;
import com.qually.qually.dto.response.*;
import com.qually.qually.mappers.AuditDisputeMapper;
import com.qually.qually.mappers.AuditSessionMapper;
import com.qually.qually.mappers.SessionScoreMapper;
import com.qually.qually.models.*;
import com.qually.qually.models.enums.AuditStatus;
import com.qually.qually.models.enums.Department;
import com.qually.qually.models.enums.ResolutionOutcome;
import com.qually.qually.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditSessionService {

    private static final Logger log = LoggerFactory.getLogger(AuditSessionService.class);

    private final AuditSessionRepository auditSessionRepository;
    private final AuditProtocolRepository auditProtocolRepository;
    private final UserRepository userRepository;
    private final LobRepository lobRepository;
    private final AuditResponseRepository auditResponseRepository;
    private final SessionScoreRepository sessionScoreRepository;
    private final AuditSessionMapper auditSessionMapper;
    private final AuditDisputeMapper auditDisputeMapper;
    private final SessionScoreMapper sessionScoreMapper;

    public AuditSessionService(AuditSessionRepository auditSessionRepository,
                               AuditProtocolRepository auditProtocolRepository,
                               UserRepository userRepository,
                               LobRepository lobRepository,
                               AuditResponseRepository auditResponseRepository,
                               SessionScoreRepository sessionScoreRepository,
                               AuditSessionMapper auditSessionMapper,
                               AuditDisputeMapper auditDisputeMapper,
                               SessionScoreMapper sessionScoreMapper) {
        this.auditSessionRepository = auditSessionRepository;
        this.auditProtocolRepository = auditProtocolRepository;
        this.userRepository = userRepository;
        this.lobRepository = lobRepository;
        this.auditResponseRepository = auditResponseRepository;
        this.sessionScoreRepository = sessionScoreRepository;
        this.auditSessionMapper = auditSessionMapper;
        this.auditDisputeMapper = auditDisputeMapper;
        this.sessionScoreMapper = sessionScoreMapper;
    }

    @Transactional
    public AuditSessionResponseDTO createSession(AuditSessionRequestDTO dto) {
        AuditProtocol protocol = auditProtocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(dto.getProtocolId())));
        User auditor = userRepository.findById(dto.getAuditorUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Auditor with ID %d not found".formatted(dto.getAuditorUserId())));
        User memberAudited = userRepository.findById(dto.getMemberAuditedUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Member audited with ID %d not found"
                                .formatted(dto.getMemberAuditedUserId())));

        if (dto.getAuditorUserId().equals(dto.getMemberAuditedUserId())) {
            throw new IllegalArgumentException(
                    "The auditor and the member audited cannot be the same person.");
        }

        Lob lob = lobRepository.findById(dto.getLobId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "LOB with ID %d not found".formatted(dto.getLobId())));

        if (!lob.getClient().getClientId().equals(protocol.getClient().getClientId())) {
            throw new IllegalArgumentException(
                    "The selected LOB does not belong to this protocol's client.");
        }

        AuditSession session = auditSessionMapper.toEntity(
                dto, protocol, auditor, memberAudited, lob);
        AuditSession saved = auditSessionRepository.save(session);

        log.info("Session {} created — protocol '{}', auditor {}, member {}, status {}",
                saved.getSessionId(), protocol.getProtocolName(),
                auditor.getUserId(), memberAudited.getUserId(), saved.getAuditStatus());

        return auditSessionMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<AuditSessionResponseDTO> getAllSessions(Integer auditorUserId,
                                                        String auditStatus,
                                                        Integer currentUserId) {
        List<AuditSession> sessions;

        if (auditorUserId != null) {
            sessions = auditSessionRepository.findByAuditor_UserId(auditorUserId);
        } else if (auditStatus != null && !auditStatus.isBlank()) {
            sessions = auditSessionRepository.findByAuditStatus(
                    AuditStatus.valueOf(auditStatus.toUpperCase()));
        } else {
            sessions = auditSessionRepository.findAll();
        }

        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null && Department.OPERATIONS.equals(
                    currentUser.getRole() != null
                            ? currentUser.getRole().getDepartment() : null)) {
                List<Integer> clientIds = currentUser.getClients().stream()
                        .map(Client::getClientId).toList();
                sessions = sessions.stream()
                        .filter(s -> clientIds.contains(
                                s.getAuditProtocol().getClient().getClientId()))
                        .toList();
            }
        }

        return sessions.stream().map(auditSessionMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public AuditSessionResponseDTO getSessionById(Long id) {
        return auditSessionRepository.findById(id)
                .map(auditSessionMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Audit session with ID %d not found".formatted(id)));
    }

    @Transactional(readOnly = true)
    public SessionResultsResponseDTO getSessionResults(Long sessionId) {
        AuditSession session = auditSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Audit session with ID %d not found".formatted(sessionId)));

        List<SessionScoreResponseDTO> scores = sessionScoreRepository
                .findByAuditSession_SessionId(sessionId)
                .stream().map(sessionScoreMapper::toDTO).toList();

        List<AuditResponseResultDTO> responses = auditResponseRepository
                .findByAuditSession_SessionId(sessionId)
                .stream().map(this::toResponseResultDTO).toList();

        return SessionResultsResponseDTO.builder()
                .session(auditSessionMapper.toDTO(session))
                .scores(scores)
                .responses(responses)
                .build();
    }

    @Transactional
    public AuditSessionResponseDTO updateSession(Long id, AuditSessionUpdateRequestDTO dto) {
        AuditSession session = auditSessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Audit session with ID %d not found".formatted(id)));

        AuditStatus previousStatus = session.getAuditStatus();

        if (dto.getAuditStatus() != null) {
            session.setAuditStatus(dto.getAuditStatus());
            if (AuditStatus.COMPLETED.equals(dto.getAuditStatus())
                    && session.getSubmittedAt() == null) {
                session.setSubmittedAt(dto.getSubmittedAt() != null
                        ? dto.getSubmittedAt() : LocalDateTime.now());
            }
        }
        if (dto.getComments() != null)    session.setComments(dto.getComments());
        if (dto.getSubmittedAt() != null) session.setSubmittedAt(dto.getSubmittedAt());

        AuditSession saved = auditSessionRepository.save(session);

        if (dto.getAuditStatus() != null && !dto.getAuditStatus().equals(previousStatus)) {
            log.info("Session {} status changed: {} → {}",
                    id, previousStatus, dto.getAuditStatus());
        }

        return auditSessionMapper.toDTO(saved);
    }

    // ── Helpers ───────────────────────────────────────────────

    private AuditResponseResultDTO toResponseResultDTO(AuditResponse response) {
        String effectiveAnswer = response.getQuestionAnswer();
        if (response.getDispute() != null
                && ResolutionOutcome.MODIFIED.equals(
                response.getDispute().getResolutionOutcome())
                && response.getDispute().getNewAnswer() != null) {
            effectiveAnswer = response.getDispute().getNewAnswer();
        }
        return AuditResponseResultDTO.builder()
                .responseId(response.getAuditResponseId())
                .questionId(response.getAuditQuestion().getQuestionId())
                .questionText(response.getAuditQuestion().getQuestionText())
                .category(response.getAuditQuestion().getCategory().name())
                .originalAnswer(response.getQuestionAnswer())
                .effectiveAnswer(effectiveAnswer)
                .responseStatus(response.getResponseStatus().name())
                .isFlagged(Boolean.TRUE.equals(response.getIsFlagged()))
                .dispute(auditDisputeMapper.toDTO(response.getDispute()))
                .build();
    }
}