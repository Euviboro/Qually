package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Flat row returned by {@code GET /api/disputes/inbox}.
 *
 * <p>Each row represents a single {@code AuditResponse} that is either
 * flagged, formally disputed, or resolved. One row per response — not per
 * session — so a session with three disputed questions produces three rows.</p>
 *
 * <p>{@code displayStatus} is a derived field computed from {@code isFlagged}
 * and {@code responseStatus} so the frontend does not need to re-derive it:
 * <ul>
 *   <li>{@code "FLAGGED"}  — isFlagged = true, responseStatus = ANSWERED</li>
 *   <li>{@code "DISPUTED"} — responseStatus = DISPUTED</li>
 *   <li>{@code "RESOLVED"} — responseStatus = RESOLVED</li>
 * </ul>
 * </p>
 */
@Getter
@Builder
public class DisputeInboxRowDTO {

    // ── Session context ───────────────────────────────────────
    private Long          sessionId;
    private LocalDateTime sessionDate;
    private String        clientName;
    private String        protocolName;
    private String        lobName;
    private String        interactionId;
    private String        memberAuditedName;

    // ── Response ──────────────────────────────────────────────
    private Long   responseId;
    private String questionText;
    private String category;
    private String originalAnswer;
    /** The answer used in scoring — may differ from original if dispute was MODIFIED. */
    private String effectiveAnswer;
    private String responseStatus;
    private Boolean isFlagged;
    /**
     * Derived display status — one of FLAGGED, DISPUTED, RESOLVED.
     * Use this for rendering badges rather than re-deriving from isFlagged + responseStatus.
     */
    private String displayStatus;

    // ── Dispute detail (null when displayStatus = FLAGGED) ────
    private Integer       disputeId;
    private String        reasonText;
    private String        disputeComment;
    private String        raisedByName;
    private LocalDateTime raisedAt;
    private String        resolutionOutcome;
    private String        resolutionNote;
}