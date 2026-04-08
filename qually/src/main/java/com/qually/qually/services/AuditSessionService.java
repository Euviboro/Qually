package com.qually.qually.services;

import com.qually.qually.dto.request.AuditSessionRequestDTO;
import com.qually.qually.dto.request.AuditSessionUpdateRequestDTO;
import com.qually.qually.dto.response.AuditSessionResponseDTO;
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

import java.util.List;

@Service
public class AuditSessionService {

    private final AuditSessionRepository auditSessionRepository;
    private final AuditProtocolRepository auditProtocolRepository;
    private final UserRepository userRepository;

    public AuditSessionService(AuditSessionRepository auditSessionRepository,
                               AuditProtocolRepository auditProtocolRepository,
                               UserRepository userRepository) {
        this.auditSessionRepository = auditSessionRepository;
        this.auditProtocolRepository = auditProtocolRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AuditSessionResponseDTO createSession(AuditSessionRequestDTO dto) {
        AuditProtocol protocol = auditProtocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException("Protocol with ID %d not found".formatted(dto.getProtocolId())));
        User auditor = userRepository.findById(dto.getAuditorEmail())
                .orElseThrow(() -> new EntityNotFoundException("User with email '%s' not found".formatted(dto.getAuditorEmail())));

        AuditSession session = AuditSession.builder()
                .auditProtocol(protocol)
                .interactionId(dto.getInteractionId())
                .auditor(auditor)
                .auditLogicType(dto.getAuditLogicType())
                .comments(dto.getComments())
                .auditStatus(AuditStatus.IN_PROGRESS)
                .build();

        return toDTO(auditSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<AuditSessionResponseDTO> getAllSessions(String auditorEmail, String auditStatus) {
        List<AuditSession> sessions;
        if (auditorEmail != null && !auditorEmail.isBlank()) {
            sessions = auditSessionRepository.findByAuditor_UserEmail(auditorEmail);
        } else if (auditStatus != null && !auditStatus.isBlank()) {
            AuditStatus status = AuditStatus.valueOf(auditStatus.toUpperCase());
            sessions = auditSessionRepository.findByAuditStatus(status);
        } else {
            sessions = auditSessionRepository.findAll();
        }
        return sessions.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public AuditSessionResponseDTO getSessionById(Long id) {
        return auditSessionRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Audit session with ID %d not found".formatted(id)));
    }

    @Transactional
    public AuditSessionResponseDTO updateSession(Long id, AuditSessionUpdateRequestDTO dto) {
        AuditSession session = auditSessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Audit session with ID %d not found".formatted(id)));

        if (dto.getAuditStatus() != null) {
            session.setAuditStatus(AuditStatus.valueOf(dto.getAuditStatus().name().toUpperCase()));
        }
        if (dto.getComments() != null) session.setComments(dto.getComments());
        if (dto.getSubmittedAt() != null) session.setSubmittedAt(dto.getSubmittedAt());

        return toDTO(auditSessionRepository.save(session));
    }

    private AuditSessionResponseDTO toDTO(AuditSession session) {
        return AuditSessionResponseDTO.builder()
                .sessionId(session.getSessionId())
                .auditStatus(session.getAuditStatus().name())
                .comments(session.getComments())
                .protocolId(session.getAuditProtocol().getProtocolId())
                .protocolName(session.getAuditProtocol().getProtocolName())
                .protocolVersion(session.getAuditProtocol().getProtocolVersion())
                .auditorEmail(session.getAuditor().getUserEmail())
                .auditorName(session.getAuditor().getFullName())
                .auditLogicType(session.getAuditLogicType())
                .startedAt(session.getStartedAt())
                .submittedAt(session.getSubmittedAt())
                .build();
    }
}