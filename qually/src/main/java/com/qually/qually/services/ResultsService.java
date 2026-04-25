package com.qually.qually.services;

import com.qually.qually.dto.response.PagedResultsResponseDTO;
import com.qually.qually.dto.response.ResultsTableRowDTO;
import com.qually.qually.models.*;
import com.qually.qually.models.enums.Department;
import com.qually.qually.models.enums.ResolutionOutcome;
import com.qually.qually.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the paginated flat rows for the Results table.
 *
 * <p><strong>Item 6 fix:</strong> the hardcoded {@code TEAM_MEMBER_HIERARCHY = 7}
 * constant used to identify Team Members has been removed. The visibility tier
 * now uses {@code role.canBeAudited} — if a user's role has this flag set, they
 * are treated as a Team Member for visibility purposes (they see only sessions
 * where they are the member audited). This means the tier assignment is driven
 * by data, not by a magic number that silently breaks when roles change.</p>
 */
@Service
public class ResultsService {

    private static final Logger log = LoggerFactory.getLogger(ResultsService.class);

    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE     = 200;

    private final AuditSessionRepository auditSessionRepository;
    private final AuditResponseRepository auditResponseRepository;
    private final SessionScoreRepository sessionScoreRepository;
    private final UserRepository userRepository;

    public ResultsService(AuditSessionRepository auditSessionRepository,
                          AuditResponseRepository auditResponseRepository,
                          SessionScoreRepository sessionScoreRepository,
                          UserRepository userRepository) {
        this.auditSessionRepository = auditSessionRepository;
        this.auditResponseRepository = auditResponseRepository;
        this.sessionScoreRepository = sessionScoreRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PagedResultsResponseDTO getResults(Integer currentUserId,
                                              Integer protocolId,
                                              Integer clientId,
                                              Integer auditorId,
                                              Integer memberId,
                                              boolean includeAnswers,
                                              int page,
                                              int size) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(currentUserId)));

        int effectiveSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, effectiveSize,
                Sort.by(Sort.Direction.DESC, "startedAt"));

        // Query 1 — paginated sessions with all associations eagerly loaded
        Page<AuditSession> sessionPage = fetchByVisibility(currentUser, pageable);
        List<AuditSession> sessions = sessionPage.getContent();

        // Context filters applied in memory (page is small, ≤ 200 rows)
        sessions = applyFilters(sessions, protocolId, clientId, auditorId, memberId);

        log.debug("ResultsService: page={} size={} visibility-count={} after-filter={}",
                page, effectiveSize, sessionPage.getTotalElements(), sessions.size());

        if (sessions.isEmpty()) {
            return PagedResultsResponseDTO.builder()
                    .content(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .currentPage(page)
                    .pageSize(effectiveSize)
                    .build();
        }

        List<Long> sessionIds = sessions.stream()
                .map(AuditSession::getSessionId)
                .toList();

        // Query 2 — bulk score fetch
        Map<Long, List<SessionScore>> scoresBySession =
                sessionScoreRepository.findByAuditSession_SessionIdIn(sessionIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getAuditSession().getSessionId()));

        // Query 3 — bulk response fetch (only when answers requested)
        Map<Long, List<AuditResponse>> responsesBySession = includeAnswers
                ? auditResponseRepository.findBySessionIdInWithDetails(sessionIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                r -> r.getAuditSession().getSessionId()))
                : Map.of();

        List<ResultsTableRowDTO> rows = sessions.stream()
                .map(s -> toRow(s,
                        scoresBySession.getOrDefault(s.getSessionId(), List.of()),
                        responsesBySession.getOrDefault(s.getSessionId(), List.of()),
                        includeAnswers))
                .toList();

        return PagedResultsResponseDTO.builder()
                .content(rows)
                .totalElements(sessionPage.getTotalElements())
                .totalPages(sessionPage.getTotalPages())
                .currentPage(page)
                .pageSize(effectiveSize)
                .build();
    }

    // ── Visibility ────────────────────────────────────────────

    /**
     * Determines which sessions the user can see.
     *
     * <p><strong>Item 6 fix:</strong> the Team Member tier was previously
     * identified by {@code hierarchyLevel >= TEAM_MEMBER_HIERARCHY (7)}.
     * It now uses {@code role.canBeAudited} — a user whose role has this flag
     * set is treated as a Team Member for visibility (they see only sessions
     * where they are the member being audited). A role rename or hierarchy
     * renumbering no longer affects this logic.</p>
     *
     * <p>Visibility tiers in order of precedence:</p>
     * <ol>
     *   <li>QA department — all sessions.</li>
     *   <li>OPERATIONS, {@code role.canBeAudited = true} — own sessions only.</li>
     *   <li>OPERATIONS, {@code role.canBeAudited = false} — client-scoped.</li>
     * </ol>
     */
    private Page<AuditSession> fetchByVisibility(User user, Pageable pageable) {
        Department dept = user.getRole() != null ? user.getRole().getDepartment() : null;

        // QA sees everything
        if (Department.QA.equals(dept)) {
            return auditSessionRepository.findAllWithDetails(pageable);
        }

        // OPERATIONS Team Member (canRaiseDispute = false) — own sessions only
        boolean isTeamMemberTier = user.getRole() != null
                && !Boolean.TRUE.equals(user.getRole().getCanRaiseDispute());

        if (isTeamMemberTier) {
            return auditSessionRepository.findByMemberAuditedWithDetails(
                    user.getUserId(), pageable);
        }

        // OPERATIONS Team Leader and above (canRaiseDispute = true) — client-scoped
        List<Integer> clientIds = user.getClients().stream()
                .map(Client::getClientId)
                .toList();
        if (clientIds.isEmpty()) {
            log.warn("OPERATIONS user {} has no client assignments — returning empty results",
                    user.getUserId());
            return Page.empty(pageable);
        }
        return auditSessionRepository.findByClientIdsWithDetails(clientIds, pageable);
    }
    // ── Filters ───────────────────────────────────────────────

    private List<AuditSession> applyFilters(List<AuditSession> sessions,
                                             Integer protocolId,
                                             Integer clientId,
                                             Integer auditorId,
                                             Integer memberId) {
        return sessions.stream()
                .filter(s -> protocolId == null ||
                        protocolId.equals(s.getAuditProtocol().getProtocolId()))
                .filter(s -> clientId == null ||
                        clientId.equals(s.getAuditProtocol().getClient().getClientId()))
                .filter(s -> auditorId == null ||
                        (s.getAuditor() != null &&
                                auditorId.equals(s.getAuditor().getUserId())))
                .filter(s -> memberId == null ||
                        (s.getMemberAuditedUser() != null &&
                                memberId.equals(s.getMemberAuditedUser().getUserId())))
                .toList();
    }

    // ── Row mapping ───────────────────────────────────────────

    private ResultsTableRowDTO toRow(AuditSession session,
                                     List<SessionScore> scores,
                                     List<AuditResponse> responses,
                                     boolean includeAnswers) {
        AuditProtocol p = session.getAuditProtocol();
        Client client   = p.getClient();
        User auditor    = session.getAuditor();
        User member     = session.getMemberAuditedUser();
        Lob lob         = session.getLob();

        return ResultsTableRowDTO.builder()
                .sessionId(session.getSessionId())
                .interactionId(session.getInteractionId())
                .sessionDate(session.getStartedAt())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .lobId(lob != null ? lob.getLobId() : null)
                .lobName(lob != null ? lob.getLobName() : null)
                .protocolId(p.getProtocolId())
                .protocolName(p.getProtocolName())
                .memberAuditedUserId(member != null ? member.getUserId() : null)
                .memberAuditedName(member != null ? member.getFullName() : null)
                .auditorUserId(auditor != null ? auditor.getUserId() : null)
                .auditorName(auditor != null ? auditor.getFullName() : null)
                .customerScore(latestScore(scores, "CUSTOMER"))
                .businessScore(latestScore(scores, "BUSINESS"))
                .complianceScore(latestScore(scores, "COMPLIANCE"))
                .auditStatus(session.getAuditStatus().name())
                .questionAnswers(includeAnswers ? buildAnswers(responses) : null)
                .build();
    }

    private Short latestScore(List<SessionScore> scores, String category) {
        return scores.stream()
                .filter(s -> category.equals(s.getCategory().name()))
                .max(Comparator.comparing(SessionScore::getCalculatedAt))
                .map(SessionScore::getScore)
                .orElse(null);
    }

    private List<ResultsTableRowDTO.QuestionAnswerDTO> buildAnswers(
            List<AuditResponse> responses) {
        return responses.stream()
                .sorted(Comparator.comparing(r -> r.getAuditQuestion().getQuestionId()))
                .map(r -> {
                    String effective = r.getQuestionAnswer();
                    if (r.getDispute() != null
                            && ResolutionOutcome.MODIFIED.equals(
                                    r.getDispute().getResolutionOutcome())
                            && r.getDispute().getNewAnswer() != null) {
                        effective = r.getDispute().getNewAnswer();
                    }
                    AuditQuestion q = r.getAuditQuestion();
                    return ResultsTableRowDTO.QuestionAnswerDTO.builder()
                            .questionId(q.getQuestionId())
                            .questionText(q.getQuestionText())
                            .category(q.getCategory().name())
                            .effectiveAnswer(effective)
                            .responseStatus(r.getResponseStatus().name())
                            .build();
                })
                .toList();
    }
}
