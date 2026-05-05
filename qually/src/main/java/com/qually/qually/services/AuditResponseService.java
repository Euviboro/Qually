package com.qually.qually.services;

import com.qually.qually.dto.request.BulkAuditAnswerRequestDTO;
import com.qually.qually.dto.request.SubattributeAnswerItemDTO;
import com.qually.qually.dto.response.AuditResponseDTO;
import com.qually.qually.models.*;
import com.qually.qually.models.enums.AuditAnswerType;
import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.AuditStatus;
import com.qually.qually.models.enums.ResponseStatus;
import com.qually.qually.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditResponseService {

    private static final Logger log = LoggerFactory.getLogger(AuditResponseService.class);

    private final AuditResponseRepository auditResponseRepository;
    private final AuditSessionRepository auditSessionRepository;
    private final AuditQuestionRepository auditQuestionRepository;
    private final SubattributeOptionRepository subattributeOptionRepository;
    private final SubattributeResponseRepository subattributeResponseRepository;
    private final AuditScoreService auditScoreService;

    public AuditResponseService(
            AuditResponseRepository auditResponseRepository,
            AuditSessionRepository auditSessionRepository,
            AuditQuestionRepository auditQuestionRepository,
            SubattributeOptionRepository subattributeOptionRepository,
            SubattributeResponseRepository subattributeResponseRepository,
            AuditScoreService auditScoreService) {
        this.auditResponseRepository = auditResponseRepository;
        this.auditSessionRepository = auditSessionRepository;
        this.auditQuestionRepository = auditQuestionRepository;
        this.subattributeOptionRepository = subattributeOptionRepository;
        this.subattributeResponseRepository = subattributeResponseRepository;
        this.auditScoreService = auditScoreService;
    }

    @Transactional
    public List<AuditResponseDTO> saveBulkResponses(BulkAuditAnswerRequestDTO dto) {
        AuditSession session = auditSessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Session with ID %d not found".formatted(dto.getSessionId())));

        // Delete subattribute responses first (FK constraint)
        subattributeResponseRepository.deleteByAuditResponse_AuditSession_SessionId(
                dto.getSessionId());
        auditResponseRepository.deleteByAuditSession_SessionId(dto.getSessionId());

        boolean isAccountability = AuditLogicType.ACCOUNTABILITY
                .equals(session.getAuditProtocol().getAuditLogicType());

        List<AuditResponseDTO> results = dto.getResponses().stream()
                .map(item -> {
                    AuditAnswerType.fromValue(item.getQuestionAnswer());

                    AuditQuestion question = auditQuestionRepository
                            .findById(item.getQuestionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Question with ID %d not found"
                                            .formatted(item.getQuestionId())));

                    AuditResponse response = auditResponseRepository.save(
                            AuditResponse.builder()
                                    .auditSession(session)
                                    .auditQuestion(question)
                                    .questionAnswer(item.getQuestionAnswer())
                                    .responseStatus(ResponseStatus.ANSWERED)
                                    .build());

                    if (item.getSubattributeAnswers() != null) {
                        saveSubattributeAnswers(response, item.getSubattributeAnswers());
                    }

                    return toDTO(response);
                })
                .toList();

        log.info("Saved {} responses for session {} (status: {})",
                results.size(), session.getSessionId(), session.getAuditStatus());

        if (AuditStatus.COMPLETED.equals(session.getAuditStatus())) {
            if (isAccountability) {
                validateAccountabilityCompleteness(session, dto);
            }
            auditScoreService.calculateAndStoreScores(session.getSessionId());
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<AuditResponseDTO> getResponsesBySession(Long sessionId) {
        return auditResponseRepository.findByAuditSession_SessionId(sessionId)
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public AuditResponseDTO getResponseById(Long id) {
        return auditResponseRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Response with ID %d not found".formatted(id)));
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * For ACCOUNTABILITY protocols: verifies that every NO answer has a subattribute
     * response recorded for its question's accountability subattribute.
     *
     * <p>Called only when the session is being submitted as COMPLETED. Drafts are
     * intentionally exempt so auditors can save progress mid-form.</p>
     *
     * @throws IllegalStateException if any NO answer is missing its accountability selection.
     */
    private void validateAccountabilityCompleteness(AuditSession session,
                                                    BulkAuditAnswerRequestDTO dto) {
        // Collect which questionIds have a NO answer in this submission
        Set<Integer> noQuestionIds = dto.getResponses().stream()
                .filter(item -> AuditAnswerType.NO.matches(item.getQuestionAnswer()))
                .map(item -> item.getQuestionId())
                .collect(Collectors.toSet());

        if (noQuestionIds.isEmpty()) return;

        // Collect all option IDs submitted in this bulk request
        Set<Long> submittedOptionIds = dto.getResponses().stream()
                .filter(item -> item.getSubattributeAnswers() != null)
                .flatMap(item -> item.getSubattributeAnswers().stream())
                .map(SubattributeAnswerItemDTO::getSubattributeOptionId)
                .collect(Collectors.toSet());

        // For each NO question, find its accountability subattribute and check coverage
        for (Integer questionId : noQuestionIds) {
            AuditQuestion question = auditQuestionRepository.findById(questionId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Question with ID %d not found".formatted(questionId)));

            boolean hasAccountabilitySubattribute = question.getSubattributes().stream()
                    .anyMatch(Subattribute::isAccountability);

            if (!hasAccountabilitySubattribute) continue; // question has no accountability sub

            // Check that at least one submitted option belongs to this question's
            // accountability subattribute
            boolean covered = question.getSubattributes().stream()
                    .filter(Subattribute::isAccountability)
                    .flatMap(sub -> sub.getSubattributeOptions().stream())
                    .map(SubattributeOption::getSubattributeOptionId)
                    .anyMatch(submittedOptionIds::contains);

            if (!covered) {
                throw new IllegalStateException(
                        "Question %d was answered NO but is missing an accountability selection. "
                                .formatted(questionId)
                                + "Please select who is accountable before submitting.");
            }
        }
    }

    private void saveSubattributeAnswers(AuditResponse response,
                                         List<SubattributeAnswerItemDTO> selections) {
        selections.stream()
                .filter(s -> s.getSubattributeOptionId() != null)
                .forEach(selection -> {
                    SubattributeOption option = subattributeOptionRepository
                            .findById(selection.getSubattributeOptionId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Subattribute option with ID %d not found"
                                            .formatted(selection.getSubattributeOptionId())));
                    subattributeResponseRepository.save(
                            SubattributeResponse.builder()
                                    .auditResponse(response)
                                    .selectedOption(option)
                                    .build());
                });
    }

    private AuditResponseDTO toDTO(AuditResponse response) {
        return AuditResponseDTO.builder()
                .auditResponseId(response.getAuditResponseId())
                .sessionId(response.getAuditSession().getSessionId())
                .questionId(response.getAuditQuestion().getQuestionId())
                .questionText(response.getAuditQuestion().getQuestionText())
                .questionAnswer(response.getQuestionAnswer())
                .build();
    }
}