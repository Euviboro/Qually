package com.qually.qually.controllers;

import com.qually.qually.dto.request.CalibrationRoundRequestDTO;
import com.qually.qually.dto.request.SubmitAnswerRequestDTO;
import com.qually.qually.dto.response.CalibrationRoundResponseDTO;
import com.qually.qually.dto.response.CalibrationSessionResponseDTO;
import com.qually.qually.services.CalibrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the calibration feature.
 *
 * <p>All endpoints read the current user's ID from the JWT security context —
 * no {@code X-User-Id} header required.</p>
 *
 * <p>Endpoints:</p>
 * <pre>
 *   POST   /api/calibration/rounds              — create a new round (QA only)
 *   GET    /api/calibration/rounds              — list visible rounds
 *   GET    /api/calibration/rounds/{id}         — round detail with answers
 *   POST   /api/calibration/rounds/{id}/close   — close and compare (QA manager)
 *   POST   /api/calibration/groups/{id}/answer  — submit answer for an interaction
 * </pre>
 */
@RestController
@RequestMapping("/api/calibration")
public class CalibrationController {

    private final CalibrationService calibrationService;

    public CalibrationController(CalibrationService calibrationService) {
        this.calibrationService = calibrationService;
    }

    /**
     * Creates a new calibration round.
     * Only QA users may create rounds.
     */
    @PostMapping("/rounds")
    public ResponseEntity<CalibrationRoundResponseDTO> createRound(
            @Valid @RequestBody CalibrationRoundRequestDTO dto) {
        return ResponseEntity.ok(
                calibrationService.createRound(dto, currentUserId()));
    }

    /**
     * Returns all calibration rounds visible to the current user.
     * Visibility is determined by role — see CalibrationService.getRounds.
     */
    @GetMapping("/rounds")
    public ResponseEntity<List<CalibrationRoundResponseDTO>> getRounds() {
        return ResponseEntity.ok(calibrationService.getRounds(currentUserId()));
    }

    /**
     * Returns full detail for a calibration round.
     * Participants see only their own answers; QA managers see all.
     */
    @GetMapping("/rounds/{id}")
    public ResponseEntity<CalibrationRoundResponseDTO> getRoundDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                calibrationService.getRoundDetail(id, currentUserId()));
    }

    /**
     * Closes a round and runs the calibration comparison.
     * Only QA users at or above the creator's manager level may close a round.
     */
    @PostMapping("/rounds/{id}/close")
    public ResponseEntity<CalibrationRoundResponseDTO> closeAndCompare(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                calibrationService.closeAndCompare(id, currentUserId()));
    }

    /**
     * Submits the current user's answer for one interaction group.
     * The round must be open and the caller must be enrolled.
     * Answers are immutable — submitting twice returns 400.
     */
    @PostMapping("/groups/{groupId}/answer")
    public ResponseEntity<CalibrationSessionResponseDTO> submitAnswer(
            @PathVariable Long groupId,
            @Valid @RequestBody SubmitAnswerRequestDTO dto) {
        return ResponseEntity.ok(
                calibrationService.submitAnswer(
                        groupId,
                        dto.getCalibrationAnswer(),
                        currentUserId()));
    }

    // ── Helper ────────────────────────────────────────────────

    private Integer currentUserId() {
        return (Integer) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}