package com.qually.qually.services;

import com.qually.qually.dto.request.AuditDisputeRequestDTO;
import com.qually.qually.dto.request.ResolveDisputeRequestDTO;
import com.qually.qually.dto.response.AuditDisputeResponseDTO;
import com.qually.qually.mappers.AuditDisputeMapper;
import com.qually.qually.models.*;
import com.qually.qually.models.enums.AuditAnswerType;
import com.qually.qually.models.enums.AuditStatus;
import com.qually.qually.models.enums.Department;
import com.qually.qually.models.enums.ResolutionOutcome;
import com.qually.qually.models.enums.ResponseStatus;
import com.qually.qually.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);

    private final AuditResponseRepository auditResponseRepository;
    private final AuditSessionRepository auditSessionRepository;
    private final AuditDisputeRepository auditDisputeRepository;
    private final DisputeReasonRepository disputeReasonRepository;
    private final UserRepository userRepository;
    private final AuditDisputeMapper auditDisputeMapper;
    private final AuditScoreService auditScoreService;

    public DisputeService(AuditResponseRepository auditResponseRepository,
                          AuditSessionRepository auditSessionRepository,
                          AuditDisputeRepository auditDisputeRepository,
                          DisputeReasonRepository disputeReasonRepository,
                          UserRepository userRepository,
                          AuditDisputeMapper auditDisputeMapper,
                          AuditScoreService auditScoreService) {
        this.auditResponseRepository = auditResponseRepository;
        this.auditSessionRepository = auditSessionRepository;
        this.auditDisputeRepository = auditDisputeRepository;
        this.disputeReasonRepository = disputeReasonRepository;
        this.userRepository = userRepository;
        this.auditDisputeMapper = auditDisputeMapper;
        this.auditScoreService = auditScoreService;
    }

    // ── Flag ──────────────────────────────────────────────────

    @Transactional
    public void flagResponse(Long responseId, Integer userId) {
        AuditResponse response = findResponse(responseId);
        User user = findUser(userId);

        if (ResponseStatus.ANSWERED != response.getResponseStatus()) {
            throw new IllegalArgumentException(
                    "Only ANSWERED responses can be flagged (current status: %s)"
                    .formatted(response.getResponseStatus()));
        }
        // YES answers cannot be disputed — use enum instead of string literal
        if (AuditAnswerType.YES.matches(response.getQuestionAnswer())) {
            throw new IllegalArgumentException("YES answers cannot be flagged.");
        }

        checkCanFlag(user, response.getAuditSession());

        response.setResponseStatus(ResponseStatus.FLAGGED);
        auditResponseRepository.save(response);
        log.info("Response {} flagged by user {}", responseId, userId);
    }

    @Transactional
    public void unflagResponse(Long responseId, Integer userId) {
        AuditResponse response = findResponse(responseId);
        User user = findUser(userId);

        if (ResponseStatus.FLAGGED != response.getResponseStatus()) {
            throw new IllegalArgumentException("Response is not flagged.");
        }

        checkCanFlag(user, response.getAuditSession());

        response.setResponseStatus(ResponseStatus.ANSWERED);
        auditResponseRepository.save(response);
        log.info("Response {} unflagged by user {}", responseId, userId);
    }

    // ── Dispute ───────────────────────────────────────────────

    @Transactional
    public AuditDisputeResponseDTO raiseDispute(AuditDisputeRequestDTO dto, Integer userId) {
        AuditResponse response = findResponse(dto.getResponseId());
        User user = findUser(userId);

        if (ResponseStatus.FLAGGED != response.getResponseStatus()) {
            throw new IllegalArgumentException("Only FLAGGED responses can be formally disputed.");
        }
        if (response.getDispute() != null) {
            throw new IllegalArgumentException("This response already has an active dispute.");
        }

        AuditSession session = response.getAuditSession();
        if (AuditStatus.COMPLETED != session.getAuditStatus()) {
            throw new IllegalArgumentException("Disputes can only be raised on COMPLETED sessions.");
        }

        checkCanRaiseDispute(user, session);

        DisputeReason reason = disputeReasonRepository.findById(dto.getReasonId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Dispute reason with ID %d not found".formatted(dto.getReasonId())));

        AuditDispute dispute = AuditDispute.builder()
                .response(response)
                .raisedBy(user)
                .reason(reason)
                .disputeComment(dto.getDisputeComment())
                .raisedAt(LocalDateTime.now())
                .build();

        auditDisputeRepository.save(dispute);
        response.setResponseStatus(ResponseStatus.DISPUTED);
        auditResponseRepository.save(response);
        session.setAuditStatus(AuditStatus.DISPUTED);
        auditSessionRepository.save(session);

        log.info("Dispute raised on response {} by user {} — reason {}",
                dto.getResponseId(), userId, dto.getReasonId());

        return auditDisputeMapper.toDTO(dispute);
    }

    // ── Resolve ───────────────────────────────────────────────

    @Transactional
    public AuditDisputeResponseDTO resolveDispute(Integer disputeId,
                                                   ResolveDisputeRequestDTO dto,
                                                   Integer userId) {
        AuditDispute dispute = auditDisputeRepository.findById(disputeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Dispute with ID %d not found".formatted(disputeId)));
        User resolver = findUser(userId);

        if (dispute.getResolutionOutcome() != null) {
            throw new IllegalArgumentException("This dispute has already been resolved.");
        }

        if (ResolutionOutcome.MODIFIED.equals(dto.getResolutionOutcome())) {
            if (dto.getNewAnswer() == null || dto.getNewAnswer().isBlank()) {
                throw new IllegalArgumentException(
                        "A new answer is required when resolution outcome is MODIFIED.");
            }
            // Validate the new answer is a known value
            AuditAnswerType.fromValue(dto.getNewAnswer()); // throws if invalid
            if (dto.getNewAnswer().equals(dispute.getResponse().getQuestionAnswer())) {
                throw new IllegalArgumentException(
                        "New answer must differ from the original answer.");
            }
        }

        AuditSession session = dispute.getResponse().getAuditSession();
        checkCanResolve(resolver, session);

        dispute.setResolvedBy(resolver);
        dispute.setResolutionDate(LocalDateTime.now());
        dispute.setResolutionNote(dto.getResolutionNote());
        dispute.setResolutionOutcome(dto.getResolutionOutcome());

        if (ResolutionOutcome.MODIFIED.equals(dto.getResolutionOutcome())) {
            dispute.setNewAnswer(dto.getNewAnswer());
        }

        auditDisputeRepository.save(dispute);
        dispute.getResponse().setResponseStatus(ResponseStatus.RESOLVED);
        auditResponseRepository.save(dispute.getResponse());

        if (!auditDisputeRepository.hasUnresolvedDisputes(session.getSessionId())) {
            closeSession(session);
        }

        if (ResolutionOutcome.MODIFIED.equals(dto.getResolutionOutcome())) {
            auditScoreService.recalculateAndStoreScores(session.getSessionId());
        }

        log.info("Dispute {} resolved by user {} — outcome {}",
                disputeId, userId, dto.getResolutionOutcome());

        return auditDisputeMapper.toDTO(dispute);
    }

    // ── Permission checks ─────────────────────────────────────

    private void checkCanFlag(User user, AuditSession session) {
        if (Department.QA.equals(getDepartment(user))) {
            throw new IllegalStateException("QA users cannot flag responses.");
        }
        Integer sessionClientId = session.getAuditProtocol().getClient().getClientId();
        boolean hasClientAccess = user.getClients().stream()
                .anyMatch(c -> c.getClientId().equals(sessionClientId));
        if (!hasClientAccess) {
            throw new IllegalStateException(
                    "User does not have access to this client's sessions.");
        }
    }

    private void checkCanRaiseDispute(User user, AuditSession session) {
        checkCanFlag(user, session);
        if (user.getRole() == null || !Boolean.TRUE.equals(user.getRole().getCanRaiseDispute())) {
            throw new IllegalStateException(
                    "Your role does not have permission to raise disputes.");
        }
    }

    private void checkCanResolve(User resolver, AuditSession session) {
        if (!Department.QA.equals(getDepartment(resolver))) {
            throw new IllegalStateException("Only QA users can resolve disputes.");
        }
        User auditor = session.getAuditor();
        if (auditor == null || auditor.getManager() == null) return;
        Integer managerLevel  = getHierarchyLevel(auditor.getManager());
        Integer resolverLevel = getHierarchyLevel(resolver);
        if (managerLevel != null && resolverLevel != null && resolverLevel > managerLevel) {
            throw new IllegalStateException(
                    "Resolver must be at or above the auditor's manager in the QA hierarchy.");
        }
    }

    // ── Session closure ───────────────────────────────────────

    private void closeSession(AuditSession session) {
        session.setAuditStatus(AuditStatus.RESOLVED);

        boolean anyModified = auditDisputeRepository.findBySessionId(session.getSessionId())
                .stream()
                .anyMatch(d -> ResolutionOutcome.MODIFIED.equals(d.getResolutionOutcome()));

        session.setResolutionOutcome(anyModified
                ? ResolutionOutcome.MODIFIED
                : ResolutionOutcome.UNCHANGED);

        auditSessionRepository.save(session);
        log.info("Session {} closed with resolution outcome {}",
                session.getSessionId(), session.getResolutionOutcome());
    }

    // ── Helpers ───────────────────────────────────────────────

    private AuditResponse findResponse(Long responseId) {
        return auditResponseRepository.findById(responseId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Response with ID %d not found".formatted(responseId)));
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(userId)));
    }

    private Department getDepartment(User user) {
        return user.getRole() != null ? user.getRole().getDepartment() : null;
    }

    private Integer getHierarchyLevel(User user) {
        return user.getRole() != null ? user.getRole().getHierarchyLevel() : null;
    }
}
