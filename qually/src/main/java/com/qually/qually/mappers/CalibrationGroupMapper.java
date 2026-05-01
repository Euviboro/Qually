package com.qually.qually.mappers;

import com.qually.qually.dto.response.CalibrationGroupResponseDTO;
import com.qually.qually.dto.response.CalibrationSessionResponseDTO;
import com.qually.qually.models.CalibrationGroup;
import com.qually.qually.models.CalibrationSession;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps {@link CalibrationGroup} entities to {@link CalibrationGroupResponseDTO}.
 *
 * <p>Visibility rules for sessions are applied here:</p>
 * <ul>
 *   <li>QA manager view — all sessions included, expert answer shown
 *       after round closes, each session includes the caller's result.</li>
 *   <li>Participant view (round open) — only the caller's own session
 *       included, no expert answer, no result.</li>
 *   <li>Participant view (round closed) — only the caller's own session,
 *       expert answer shown for side-by-side comparison, result shown.</li>
 * </ul>
 */
@Component
public class CalibrationGroupMapper {

    private final CalibrationSessionMapper sessionMapper;

    public CalibrationGroupMapper(CalibrationSessionMapper sessionMapper) {
        this.sessionMapper = sessionMapper;
    }

    /**
     * Maps a group for a QA manager — all sessions visible, expert
     * identity accessible (caller handles whether to expose it in the
     * parent DTO).
     *
     * @param group       The group entity with sessions loaded.
     * @param expertAnswer The expert's answer — null if round is open.
     */
    public CalibrationGroupResponseDTO toDTOForManager(CalibrationGroup group,
                                                       String expertAnswer) {
        List<CalibrationSessionResponseDTO> sessions = group.getSessions().stream()
                .map(s -> sessionMapper.toDTO(s, expertAnswer))
                .toList();

        return CalibrationGroupResponseDTO.builder()
                .groupId(group.getGroupId())
                .interactionId(group.getInteractionId())
                .isCalibrated(group.getIsCalibrated())
                .expertAnswer(expertAnswer)
                .sessions(sessions)
                .build();
    }

    /**
     * Maps a group for a participant — only their own session is included.
     *
     * @param group        The group entity with sessions loaded.
     * @param callerId     The user ID of the caller.
     * @param expertAnswer The expert's answer — null when round is open,
     *                     populated when round is closed.
     * @param roundOpen    Whether the round is currently open.
     */
    public CalibrationGroupResponseDTO toDTOForParticipant(CalibrationGroup group,
                                                           Integer callerId,
                                                           String expertAnswer,
                                                           boolean roundOpen) {
        // Find the caller's own session for this group
        List<CalibrationSessionResponseDTO> ownSession = group.getSessions().stream()
                .filter(s -> s.getUser().getUserId().equals(callerId))
                .map(s -> sessionMapper.toDTO(s, roundOpen ? null : expertAnswer))
                .toList();

        return CalibrationGroupResponseDTO.builder()
                .groupId(group.getGroupId())
                .interactionId(group.getInteractionId())
                .isCalibrated(roundOpen ? null : group.getIsCalibrated())
                .expertAnswer(roundOpen ? null : expertAnswer)
                .sessions(ownSession)
                .build();
    }

    /**
     * Builds a map of groupId → expert answer from a list of groups
     * and the expert's sessions. Used by the service layer before
     * calling the per-group mapping methods.
     *
     * @param groups         All groups in the round.
     * @param expertSessions All sessions submitted by the expert.
     * @return Map of groupId → expert's calibration answer (null if not yet answered).
     */
    public Map<Long, String> buildExpertAnswerMap(List<CalibrationGroup> groups,
                                                  List<CalibrationSession> expertSessions) {
        Map<Long, String> expertByGroup = expertSessions.stream()
                .collect(Collectors.toMap(
                        s -> s.getGroup().getGroupId(),
                        CalibrationSession::getCalibrationAnswer));

        return groups.stream()
                .collect(Collectors.toMap(
                        CalibrationGroup::getGroupId,
                        g -> expertByGroup.getOrDefault(g.getGroupId(), null)));
    }
}