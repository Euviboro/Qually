package com.qually.qually.services;

import com.qually.qually.models.SubattributeResponse;
import com.qually.qually.models.AuditResponse;
import com.qually.qually.models.AuditSession;
import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.CopcCategory;
import com.qually.qually.repositories.AttributeResponseRepository;
import com.qually.qually.repositories.AuditResponseRepository;
import com.qually.qually.repositories.AuditSessionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditScoreService {

    private final AuditSessionRepository auditSessionRepository;
    private final AuditResponseRepository auditResponseRepository;
    private final AttributeResponseRepository attributeResponseRepository;

    public AuditScoreService(AuditSessionRepository auditSessionRepository,
                             AuditResponseRepository auditResponseRepository,
                             AttributeResponseRepository attributeResponseRepository) {
        this.auditSessionRepository = auditSessionRepository;
        this.auditResponseRepository = auditResponseRepository;
        this.attributeResponseRepository = attributeResponseRepository;
    }

    @Transactional(readOnly = true)
    public Map<CopcCategory, String> calculateScores(Long sessionId) {
        AuditSession session = auditSessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Audit session with ID %d not found".formatted(sessionId)));

        List<AuditResponse> auditResponses = auditResponseRepository.findByAuditSession_SessionId(sessionId);

        Map<CopcCategory, List<AuditResponse>> groupedResponsesMap = new HashMap<>();
        for (AuditResponse response : auditResponses) {
            CopcCategory category = response.getAuditQuestion().getCategory();
            groupedResponsesMap.computeIfAbsent(category, k -> new ArrayList<>()).add(response);
        }

        Map<CopcCategory, String> finalScores = new HashMap<>();
        for (CopcCategory category : CopcCategory.values()) {
            List<AuditResponse> categoryResponses = groupedResponsesMap.getOrDefault(category, List.of());
            finalScores.put(category, evaluateCategory(categoryResponses, session.getAuditLogicType()));
        }

        return finalScores;
    }

    private String evaluateCategory(List<AuditResponse> categoryResponses, AuditLogicType auditLogicType) {
        if (categoryResponses.isEmpty()) return "N/A";

        boolean allAreNA = true;
        boolean hasEffectiveNo = false;

        for (AuditResponse response : categoryResponses) {
            String answer = response.getQuestionAnswer().toUpperCase();

            if (!answer.equals("N/A")) {
                allAreNA = false;
            }

            if (answer.equals("NO")) {
                if (auditLogicType == AuditLogicType.ACCOUNTABILITY) {
                    if (isOurFault(response)) {
                        hasEffectiveNo = true;
                        break;
                    }
                } else {
                    hasEffectiveNo = true;
                    break;
                }
            }
        }

        if (hasEffectiveNo) return "0";
        if (allAreNA) return "N/A";
        return "100";
    }

    private boolean isOurFault(AuditResponse response) {
        List<SubattributeResponse> attrResponses = attributeResponseRepository
                .findByAuditResponse_AuditResponseId(response.getAuditResponseId());

        for (SubattributeResponse attrResponse : attrResponses) {
            String attrName = attrResponse.getSubattribute().getSubattributeText().toLowerCase();
            if (attrName.contains("accountability")) {
                return attrResponse.getAnswerValue().equalsIgnoreCase("Ours");
            }
        }
        return false;
    }
}