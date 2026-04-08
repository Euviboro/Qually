package com.qually.qually.services;

import com.qually.qually.dto.request.AuditResponseItemDTO;
import com.qually.qually.dto.request.BulkAuditAnswerRequestDTO;
import com.qually.qually.dto.response.AuditResponseDTO;
import com.qually.qually.models.AuditQuestion;
import com.qually.qually.models.AuditResponse;
import com.qually.qually.models.AuditSession;
import com.qually.qually.repositories.AuditQuestionRepository;
import com.qually.qually.repositories.AuditResponseRepository;
import com.qually.qually.repositories.AuditSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditResponseService {

    private final AuditResponseRepository auditResponseRepository;
    private final AuditSessionRepository auditSessionRepository;
    private final AuditQuestionRepository auditQuestionRepository;

    public AuditResponseService(AuditResponseRepository auditResponseRepository,
                                AuditSessionRepository auditSessionRepository,
                                AuditQuestionRepository auditQuestionRepository) {
        this.auditResponseRepository = auditResponseRepository;
        this.auditSessionRepository = auditSessionRepository;
        this.auditQuestionRepository = auditQuestionRepository;
    }

    @Transactional
    public List<AuditResponseDTO> submitBulkResponses(BulkAuditAnswerRequestDTO dto) {
        AuditSession session = auditSessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new EntityNotFoundException("Session with ID %d not found".formatted(dto.getSessionId())));

        List<AuditResponse> responses = dto.getResponses().stream()
                .map(item -> buildResponse(item, session))
                .toList();

        return auditResponseRepository.saveAll(responses).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditResponseDTO> getResponsesBySession(Long sessionId) {
        return auditResponseRepository.findByAuditSession_SessionId(sessionId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditResponseDTO getResponseById(Long id) {
        return auditResponseRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Audit response with ID %d not found".formatted(id)));
    }

    private AuditResponse buildResponse(AuditResponseItemDTO item, AuditSession session) {
        AuditQuestion question = auditQuestionRepository.findById(item.getQuestionId())
                .orElseThrow(() -> new EntityNotFoundException("Question with ID %d not found".formatted(item.getQuestionId())));

        return AuditResponse.builder()
                .auditSession(session)
                .auditQuestion(question)
                .questionAnswer(item.getQuestionAnswer())
                .build();
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