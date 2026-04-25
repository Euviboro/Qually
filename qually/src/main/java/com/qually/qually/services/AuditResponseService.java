package com.qually.qually.services;

import com.qually.qually.dto.request.BulkAuditAnswerRequestDTO;
import com.qually.qually.dto.request.SubattributeAnswerItemDTO;
import com.qually.qually.dto.response.AuditResponseDTO;
import com.qually.qually.models.*;
import com.qually.qually.models.enums.AuditAnswerType;
import com.qually.qually.models.enums.AuditStatus;
import com.qually.qually.models.enums.ResponseStatus;
import com.qually.qually.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        List<AuditResponseDTO> results = dto.getResponses().stream()
                .map(item -> {
                    // Validate the answer value using the enum
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
