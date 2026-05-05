package com.qually.qually.services;

import com.qually.qually.models.AuditResponse;
import com.qually.qually.models.AuditSession;
import com.qually.qually.models.SessionScore;
import com.qually.qually.models.Subattribute;
import com.qually.qually.models.SubattributeResponse;
import com.qually.qually.models.enums.AuditAnswerType;
import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.CopcCategory;
import com.qually.qually.repositories.AuditResponseRepository;
import com.qually.qually.repositories.AuditSessionRepository;
import com.qually.qually.repositories.SessionScoreRepository;
import com.qually.qually.repositories.SubattributeResponseRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates and stores COPC scores for audit sessions.
 *
 * <p><strong>STANDARD scoring rule (COPC):</strong></p>
 * <ul>
 *   <li>Score is 0 or 100 per category.</li>
 *   <li>{@link AuditAnswerType#NA} answers are excluded from the calculation.</li>
 *   <li>All applicable (non-N/A) answers YES → 100.</li>
 *   <li>Any applicable answer NO → 0.</li>
 *   <li>All answers N/A → 100 (no applicable questions in that category).</li>
 * </ul>
 *
 * <p><strong>ACCOUNTABILITY scoring rule:</strong></p>
 * <ul>
 *   <li>Same base rules, but NO answers are conditionally excused.</li>
 *   <li>A NO answer is only counted if the auditor selected an accountability option
 *       where {@code isCompanyAccountable = true}.</li>
 *   <li>A NO with a non-company-accountable option (e.g. "Agent", "External") is
 *       treated as N/A — excluded from the denominator and does not fail the category.</li>
 *   <li>A NO with no accountability selection at all is treated as a company-accountable
 *       failure (defensive default — submission should have been blocked by the service
 *       layer, but this guards against data integrity issues).</li>
 * </ul>
 */
@Service
public class AuditScoreService {

    private static final Logger log = LoggerFactory.getLogger(AuditScoreService.class);

    private final AuditResponseRepository auditResponseRepository;
    private final AuditSessionRepository auditSessionRepository;
    private final SessionScoreRepository sessionScoreRepository;
    private final SubattributeResponseRepository subattributeResponseRepository;

    public AuditScoreService(AuditResponseRepository auditResponseRepository,
                             AuditSessionRepository auditSessionRepository,
                             SessionScoreRepository sessionScoreRepository,
                             SubattributeResponseRepository subattributeResponseRepository) {
        this.auditResponseRepository = auditResponseRepository;
        this.auditSessionRepository = auditSessionRepository;
        this.sessionScoreRepository = sessionScoreRepository;
        this.subattributeResponseRepository = subattributeResponseRepository;
    }

    /**
     * Calculates and stores the three COPC scores for a newly completed session.
     * Called by {@link AuditResponseService} after bulk responses are persisted.
     */
    @Transactional
    public void calculateAndStoreScores(Long sessionId) {
        persistScores(sessionId, false);
    }

    /**
     * Recalculates scores after a dispute resolves as MODIFIED.
     * Inserts new rows with {@code isPostDispute = true} — originals are preserved.
     */
    @Transactional
    public void recalculateAndStoreScores(Long sessionId) {
        persistScores(sessionId, true);
    }

    // ── Internal ──────────────────────────────────────────────

    private void persistScores(Long sessionId, boolean isPostDispute) {
        AuditSession session = auditSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Session with ID %d not found".formatted(sessionId)));

        boolean isAccountability = AuditLogicType.ACCOUNTABILITY
                .equals(session.getAuditProtocol().getAuditLogicType());

        List<AuditResponse> responses =
                auditResponseRepository.findByAuditSession_SessionId(sessionId);

        // For ACCOUNTABILITY mode: build a set of auditResponseIds whose NO is excused
        // (i.e. the selected accountability option is NOT company-accountable)
        Set<Long> excusedResponseIds = isAccountability
                ? buildExcusedResponseIds(sessionId)
                : Set.of();

        Map<CopcCategory, List<String>> answersByCategory =
                groupEffectiveAnswers(responses, excusedResponseIds);

        List<SessionScore> scores = new ArrayList<>();
        StringBuilder scoreLog = new StringBuilder();

        for (CopcCategory category : CopcCategory.values()) {
            List<String> answers = answersByCategory.getOrDefault(category, List.of());

            // Exclude N/A from the denominator
            List<String> applicable = answers.stream()
                    .filter(a -> !AuditAnswerType.NA.matches(a))
                    .toList();

            // Any NO in the applicable answers fails the category
            short score = applicable.stream().anyMatch(AuditAnswerType.NO::matches)
                    ? (short) 0
                    : (short) 100;

            scores.add(SessionScore.builder()
                    .auditSession(session)
                    .category(category)
                    .score(score)
                    .isPostDispute(isPostDispute)
                    .calculatedAt(LocalDateTime.now())
                    .build());

            scoreLog.append(category.name()).append("=").append(score).append(" ");
        }

        sessionScoreRepository.saveAll(scores);

        log.info("Scores calculated for session {} (postDispute={}, mode={}): {}",
                sessionId, isPostDispute,
                isAccountability ? "ACCOUNTABILITY" : "STANDARD",
                scoreLog.toString().trim());
    }

    /**
     * Builds the set of {@code auditResponseId}s whose NO answer is excused because
     * the auditor selected a non-company-accountable option on the accountability subattribute.
     *
     * <p>A response is excused when:</p>
     * <ol>
     *   <li>It has a subattribute response linked to an accountability subattribute
     *       ({@code isAccountabilitySubattribute = true}).</li>
     *   <li>The selected option has {@code isCompanyAccountable = false}.</li>
     * </ol>
     *
     * <p>If the NO answer has no accountability subattribute response at all, it is
     * NOT excused — it counts as a company-accountable failure.</p>
     */
    private Set<Long> buildExcusedResponseIds(Long sessionId) {
        List<SubattributeResponse> subResponses =
                subattributeResponseRepository.findBySessionIdWithDetails(sessionId);

        return subResponses.stream()
                .filter(sr -> {
                    Subattribute sub = sr.getSelectedOption().getSubattribute();
                    boolean isAccountabilitySub = sub.isAccountability();
                    boolean isCompanyAccountable = sr.getSelectedOption().isCompanyAccountable();
                    // Excused = accountability subattribute selected + NOT company accountable
                    return isAccountabilitySub && !isCompanyAccountable;
                })
                .map(sr -> sr.getAuditResponse().getAuditResponseId())
                .collect(Collectors.toSet());
    }

    private Map<CopcCategory, List<String>> groupEffectiveAnswers(
            List<AuditResponse> responses,
            Set<Long> excusedResponseIds) {
        return responses.stream().collect(
                Collectors.groupingBy(
                        r -> r.getAuditQuestion().getCategory(),
                        Collectors.mapping(
                                r -> effectiveAnswer(r, excusedResponseIds),
                                Collectors.toList()
                        )
                )
        );
    }

    /**
     * Returns the answer to use for scoring.
     *
     * <p>Priority order:</p>
     * <ol>
     *   <li>If a MODIFIED dispute exists, the dispute's new answer wins.</li>
     *   <li>If the response is in the excused set (ACCOUNTABILITY mode only), return N/A.</li>
     *   <li>Otherwise, return the recorded answer as-is.</li>
     * </ol>
     */
    private String effectiveAnswer(AuditResponse response, Set<Long> excusedResponseIds) {
        // Dispute override takes priority
        if (response.getDispute() != null
                && com.qually.qually.models.enums.ResolutionOutcome.MODIFIED.equals(
                response.getDispute().getResolutionOutcome())
                && response.getDispute().getNewAnswer() != null) {
            return response.getDispute().getNewAnswer();
        }

        // ACCOUNTABILITY: excuse non-company-accountable NOs
        if (excusedResponseIds.contains(response.getAuditResponseId())) {
            return AuditAnswerType.NA.getValue();
        }

        return response.getQuestionAnswer();
    }
}