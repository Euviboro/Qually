package com.qually.qually.services;

import com.qually.qually.models.AuditResponse;
import com.qually.qually.models.AuditSession;
import com.qually.qually.models.SessionScore;
import com.qually.qually.models.enums.AuditAnswerType;
import com.qually.qually.models.enums.CopcCategory;
import com.qually.qually.repositories.AuditResponseRepository;
import com.qually.qually.repositories.AuditSessionRepository;
import com.qually.qually.repositories.SessionScoreRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates and stores COPC scores for audit sessions.
 *
 * <p><strong>Scoring rule (COPC standard):</strong></p>
 * <ul>
 *   <li>Score is 0 or 100 per category.</li>
 *   <li>{@link AuditAnswerType#NA} answers are excluded from the calculation.</li>
 *   <li>All applicable (non-N/A) answers YES → 100.</li>
 *   <li>Any applicable answer NO → 0.</li>
 *   <li>All answers N/A → 100 (no applicable questions in that category).</li>
 * </ul>
 */
@Service
public class AuditScoreService {

    private static final Logger log = LoggerFactory.getLogger(AuditScoreService.class);

    private final AuditResponseRepository auditResponseRepository;
    private final AuditSessionRepository auditSessionRepository;
    private final SessionScoreRepository sessionScoreRepository;

    public AuditScoreService(AuditResponseRepository auditResponseRepository,
                             AuditSessionRepository auditSessionRepository,
                             SessionScoreRepository sessionScoreRepository) {
        this.auditResponseRepository = auditResponseRepository;
        this.auditSessionRepository = auditSessionRepository;
        this.sessionScoreRepository = sessionScoreRepository;
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

        List<AuditResponse> responses =
                auditResponseRepository.findByAuditSession_SessionId(sessionId);

        Map<CopcCategory, List<String>> answersByCategory = groupEffectiveAnswers(responses);

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

        log.info("Scores calculated for session {} (postDispute={}): {}",
                sessionId, isPostDispute, scoreLog.toString().trim());
    }

    private Map<CopcCategory, List<String>> groupEffectiveAnswers(
            List<AuditResponse> responses) {
        return responses.stream().collect(
                Collectors.groupingBy(
                        r -> r.getAuditQuestion().getCategory(),
                        Collectors.mapping(this::effectiveAnswer, Collectors.toList())
                )
        );
    }

    private String effectiveAnswer(AuditResponse response) {
        if (response.getDispute() != null
                && com.qually.qually.models.enums.ResolutionOutcome.MODIFIED.equals(
                        response.getDispute().getResolutionOutcome())
                && response.getDispute().getNewAnswer() != null) {
            return response.getDispute().getNewAnswer();
        }
        return response.getQuestionAnswer();
    }
}
