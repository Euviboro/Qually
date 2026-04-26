package com.qually.qually.services;

import com.qually.qually.dto.request.AuditDisputeRequestDTO;
import com.qually.qually.dto.request.ResolveDisputeRequestDTO;
import com.qually.qually.dto.response.AuditDisputeResponseDTO;
import com.qually.qually.dto.response.DisputeInboxRowDTO;
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
import java.util.List;
import java.util.stream.Stream;

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

    // ── Inbox ─────────────────────────────────────────────────

    /**
     * Returns the disputes inbox for the given user, scoped by their role.
     *
     * <p><strong>Visibility tiers:</strong></p>
     * <ul>
     *   <li><strong>OPERATIONS Team Member</strong> ({@code canRaiseDispute = false}) —
     *       responses in sessions where they are the member audited, that are
     *       flagged, disputed, or resolved.</li>
     *   <li><strong>OPERATIONS Team Leader+</strong> ({@code canRaiseDispute = true}) —
     *       same but for all direct reports (members whose {@code manager_id} is
     *       this user), scoped to the user's assigned clients.</li>
     *   <li><strong>QA</strong> — disputed and resolved responses from sessions
     *       they personally audited. QA users who have subordinate QA auditors
     *       (their userId appears as manager_id for another QA user) also see
     *       those subordinates' audited sessions.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public List<DisputeInboxRowDTO> getInbox(Integer userId) {
        User user = findUser(userId);
        Department dept = getDepartment(user);

        List<AuditResponse> responses;

        if (Department.QA.equals(dept)) {
            // Collect this user's ID plus any QA subordinates' IDs
            List<Integer> auditorIds = Stream.concat(
                    Stream.of(userId),
                    userRepository.findByManager_UserId(userId).stream()
                            .filter(u -> Department.QA.equals(getDepartment(u)))
                            .map(User::getUserId)
            ).toList();

            responses = auditResponseRepository.findInboxByAuditors(auditorIds);

        } else if (Boolean.TRUE.equals(user.getRole() != null
                ? user.getRole().getCanRaiseDispute() : false)) {
            // OPERATIONS TL+ — direct reports in their assigned clients
            List<Integer> clientIds = user.getClients().stream()
                    .map(Client::getClientId).toList();
            responses = auditResponseRepository.findInboxByManagedMembers(userId, clientIds);

        } else {
            // OPERATIONS Team Member — own audited sessions
            responses = auditResponseRepository.findInboxByMemberAudited(userId);
        }

        return responses.stream().map(this::toInboxRow).toList();
    }

    // ── Flag ──────────────────────────────────────────────────

    @Transactional
    public void flagResponse(Long responseId, Integer userId) {
        AuditResponse response = findResponse(responseId);
        User user = findUser(userId);

        if (Boolean.TRUE.equals(response.getIsFlagged())) {
            throw new IllegalArgumentException("Response is already flagged.");
        }
        if (ResponseStatus.ANSWERED != response.getResponseStatus()) {
            throw new IllegalArgumentException(
                    "Only ANSWERED responses can be flagged (current status: %s)"
                            .formatted(response.getResponseStatus()));
        }
        if (AuditAnswerType.YES.matches(response.getQuestionAnswer())) {
            throw new IllegalArgumentException("YES answers cannot be flagged.");
        }

        checkCanFlag(user, response.getAuditSession());

        response.setIsFlagged(true);
        auditResponseRepository.save(response);
        log.info("Response {} flagged by user {}", responseId, userId);
    }

    @Transactional
    public void unflagResponse(Long responseId, Integer userId) {
        AuditResponse response = findResponse(responseId);
        User user = findUser(userId);

        if (!Boolean.TRUE.equals(response.getIsFlagged())) {
            throw new IllegalArgumentException("Response is not flagged.");
        }

        checkCanFlag(user, response.getAuditSession());

        response.setIsFlagged(false);
        auditResponseRepository.save(response);
        log.info("Response {} unflagged by user {}", responseId, userId);
    }

    // ── Dispute ───────────────────────────────────────────────

    @Transactional
    public AuditDisputeResponseDTO raiseDispute(AuditDisputeRequestDTO dto, Integer userId) {
        AuditResponse response = findResponse(dto.getResponseId());
        User user = findUser(userId);

        if (!Boolean.TRUE.equals(response.getIsFlagged())) {
            throw new IllegalArgumentException("Only flagged responses can be formally disputed.");
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
        response.setIsFlagged(false);
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
            AuditAnswerType.fromValue(dto.getNewAnswer());
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

    // ── Row mapping ───────────────────────────────────────────

    private DisputeInboxRowDTO toInboxRow(AuditResponse r) {
        AuditSession  session  = r.getAuditSession();
        AuditProtocol protocol = session.getAuditProtocol();
        AuditDispute  dispute  = r.getDispute();
        Lob           lob      = session.getLob();
        User          member   = session.getMemberAuditedUser();

        String effectiveAnswer = r.getQuestionAnswer();
        if (dispute != null
                && ResolutionOutcome.MODIFIED.equals(dispute.getResolutionOutcome())
                && dispute.getNewAnswer() != null) {
            effectiveAnswer = dispute.getNewAnswer();
        }

        String displayStatus = Boolean.TRUE.equals(r.getIsFlagged())
                ? "FLAGGED"
                : r.getResponseStatus().name();

        return DisputeInboxRowDTO.builder()
                .sessionId(session.getSessionId())
                .sessionDate(session.getStartedAt())
                .clientName(protocol.getClient().getClientName())
                .protocolName(protocol.getProtocolName())
                .lobName(lob != null ? lob.getLobName() : null)
                .interactionId(session.getInteractionId())
                .memberAuditedName(member != null ? member.getFullName() : null)
                .responseId(r.getAuditResponseId())
                .questionText(r.getAuditQuestion().getQuestionText())
                .category(r.getAuditQuestion().getCategory().name())
                .originalAnswer(r.getQuestionAnswer())
                .effectiveAnswer(effectiveAnswer)
                .responseStatus(r.getResponseStatus().name())
                .isFlagged(Boolean.TRUE.equals(r.getIsFlagged()))
                .displayStatus(displayStatus)
                .disputeId(dispute != null ? dispute.getDisputeId() : null)
                .reasonText(dispute != null && dispute.getReason() != null
                        ? dispute.getReason().getReasonText() : null)
                .disputeComment(dispute != null ? dispute.getDisputeComment() : null)
                .raisedByName(dispute != null && dispute.getRaisedBy() != null
                        ? dispute.getRaisedBy().getFullName() : null)
                .raisedAt(dispute != null ? dispute.getRaisedAt() : null)
                .resolutionOutcome(dispute != null && dispute.getResolutionOutcome() != null
                        ? dispute.getResolutionOutcome().name() : null)
                .resolutionNote(dispute != null ? dispute.getResolutionNote() : null)
                .build();
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