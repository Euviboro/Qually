package com.qually.qually.services;

import com.qually.qually.dto.request.AuditSessionRequestDTO;
import com.qually.qually.dto.request.AuditSessionUpdateRequestDTO;
import com.qually.qually.dto.response.AuditSessionResponseDTO;
import com.qually.qually.mappers.AuditSessionMapper;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.AuditSession;
import com.qually.qually.models.User;
import com.qually.qually.models.enums.AuditStatus;
import com.qually.qually.repositories.AuditProtocolRepository;
import com.qually.qually.repositories.AuditSessionRepository;
import com.qually.qually.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for CRUD operations on {@link AuditSession} entities.
 *
 * <p><strong>Architecture alignment:</strong> the inline {@code toDTO} method
 * has been extracted into {@link AuditSessionMapper}, following the same pattern
 * as {@link AuditProtocolService} / {@link AuditProtocolMapper} and
 * {@link AuditQuestionService} / {@link AuditQuestionMapper}. The service is
 * now purely responsible for orchestrating repository calls and passing resolved
 * entities to the mapper.</p>
 *
 * <p><strong>Schema changes:</strong></p>
 * <ul>
 *   <li>Auditor is looked up by {@code auditorUserId} (Integer) via
 *       {@code userRepository.findById}, not by email.</li>
 *   <li>{@code getAllSessions} filters by {@code auditorUserId} (Integer)
 *       instead of {@code auditorEmail}.</li>
 * </ul>
 */
@Service
public class AuditSessionService {

    private final AuditSessionRepository auditSessionRepository;
    private final AuditProtocolRepository auditProtocolRepository;
    private final UserRepository userRepository;
    private final AuditSessionMapper auditSessionMapper;

    public AuditSessionService(AuditSessionRepository auditSessionRepository,
                               AuditProtocolRepository auditProtocolRepository,
                               UserRepository userRepository,
                               AuditSessionMapper auditSessionMapper) {
        this.auditSessionRepository = auditSessionRepository;
        this.auditProtocolRepository = auditProtocolRepository;
        this.userRepository = userRepository;
        this.auditSessionMapper = auditSessionMapper;
    }

    /**
     * Creates a new audit session.
     *
     * <p>Status defaults to {@link AuditStatus#DRAFT} when not provided.
     * {@code submittedAt} is auto-set when status is {@code COMPLETED} —
     * this logic lives in {@link AuditSessionMapper#toEntity}.</p>
     *
     * @param dto Session creation payload.
     * @return The persisted session DTO.
     * @throws EntityNotFoundException if the protocol or auditor does not exist.
     */
    @Transactional
    public AuditSessionResponseDTO createSession(AuditSessionRequestDTO dto) {
        AuditProtocol protocol = auditProtocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(dto.getProtocolId())));
        User auditor = userRepository.findById(dto.getAuditorUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(dto.getAuditorUserId())));

        AuditSession session = auditSessionMapper.toEntity(dto, protocol, auditor);
        return auditSessionMapper.toDTO(auditSessionRepository.save(session));
    }

    /**
     * Returns all sessions, optionally filtered by auditor user ID or status.
     *
     * @param auditorUserId When non-null, filters to sessions by this auditor.
     * @param auditStatus   When non-null, filters to sessions with this status string.
     */
    @Transactional(readOnly = true)
    public List<AuditSessionResponseDTO> getAllSessions(Integer auditorUserId, String auditStatus) {
        List<AuditSession> sessions;
        if (auditorUserId != null) {
            sessions = auditSessionRepository.findByAuditor_UserId(auditorUserId);
        } else if (auditStatus != null && !auditStatus.isBlank()) {
            sessions = auditSessionRepository.findByAuditStatus(
                    AuditStatus.valueOf(auditStatus.toUpperCase()));
        } else {
            sessions = auditSessionRepository.findAll();
        }
        return sessions.stream().map(auditSessionMapper::toDTO).toList();
    }

    /**
     * Returns a single session by its ID.
     *
     * @throws EntityNotFoundException if no session with this ID exists.
     */
    @Transactional(readOnly = true)
    public AuditSessionResponseDTO getSessionById(Long id) {
        return auditSessionRepository.findById(id)
                .map(auditSessionMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Audit session with ID %d not found".formatted(id)));
    }

    /**
     * Partially updates an existing session (status, comments, submittedAt).
     *
     * <p>Transitioning to {@link AuditStatus#COMPLETED} auto-sets
     * {@code submittedAt} to now if not already set.</p>
     *
     * @throws EntityNotFoundException if no session with this ID exists.
     */
    @Transactional
    public AuditSessionResponseDTO updateSession(Long id, AuditSessionUpdateRequestDTO dto) {
        AuditSession session = auditSessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Audit session with ID %d not found".formatted(id)));

        if (dto.getAuditStatus() != null) {
            session.setAuditStatus(dto.getAuditStatus());
            if (AuditStatus.COMPLETED.equals(dto.getAuditStatus()) && session.getSubmittedAt() == null) {
                session.setSubmittedAt(
                        dto.getSubmittedAt() != null ? dto.getSubmittedAt() : LocalDateTime.now());
            }
        }
        if (dto.getComments() != null)    session.setComments(dto.getComments());
        if (dto.getSubmittedAt() != null) session.setSubmittedAt(dto.getSubmittedAt());

        return auditSessionMapper.toDTO(auditSessionRepository.save(session));
    }
}
