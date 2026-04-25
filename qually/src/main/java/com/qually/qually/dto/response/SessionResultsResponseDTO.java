package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Composite response for {@code GET /api/sessions/{id}/results}.
 * Provides all data the Session Results page needs in a single request.
 */
@Getter
@Builder
public class SessionResultsResponseDTO {
    private AuditSessionResponseDTO session;
    private List<SessionScoreResponseDTO> scores;
    private List<AuditResponseResultDTO> responses;
}
