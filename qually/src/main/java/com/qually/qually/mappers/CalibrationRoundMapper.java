package com.qually.qually.mappers;

import com.qually.qually.dto.response.CalibrationGroupResponseDTO;
import com.qually.qually.dto.response.CalibrationParticipantResponseDTO;
import com.qually.qually.dto.response.CalibrationRoundResponseDTO;
import com.qually.qually.models.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps {@link CalibrationRound} entities to {@link CalibrationRoundResponseDTO}.
 *
 * <p>Two mapping paths:</p>
 * <ul>
 *   <li>{@link #toDTOForManager} — full visibility: all sessions, expert
 *       identity exposed on participants, expert answers shown after close.</li>
 *   <li>{@link #toDTOForParticipant} — restricted visibility: only the
 *       caller's own sessions, expert identity hidden, expert answer shown
 *       only after the round closes.</li>
 * </ul>
 *
 * <p>For the list view (no groups needed), use {@link #toSummaryDTO} which
 * omits groups and participant sessions for performance.</p>
 */
@Component
public class CalibrationRoundMapper {

    private final CalibrationGroupMapper groupMapper;

    public CalibrationRoundMapper(CalibrationGroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

    /**
     * Summary DTO for the rounds list — no groups or session detail.
     * Used for both manager and participant list views.
     *
     * @param round          The round entity.
     * @param participants   All enrolled participants.
     * @param answeredByUser Map of userId → count of groups answered.
     * @param totalGroups    Total number of groups in the round.
     * @param callerId       The calling user's ID.
     * @param isManager      Whether the caller is a QA manager.
     */
    public CalibrationRoundResponseDTO toSummaryDTO(CalibrationRound round,
                                                    List<CalibrationParticipant> participants,
                                                    Map<Integer, Long> answeredByUser,
                                                    int totalGroups,
                                                    Integer callerId,
                                                    boolean isManager) {
        return CalibrationRoundResponseDTO.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .clientId(round.getClient().getClientId())
                .clientName(round.getClient().getClientName())
                .protocolId(round.getProtocol().getProtocolId())
                .protocolName(round.getProtocol().getProtocolName())
                .questionId(round.getQuestion().getQuestionId())
                .questionText(round.getQuestion().getQuestionText())
                .category(round.getQuestion().getCategory().name())
                .isOpen(round.getIsOpen())
                .isCalibrated(round.getIsCalibrated())
                .createdByName(round.getCreatedBy().getFullName())
                .createdAt(round.getCreatedAt())
                .participants(toParticipantDTOs(participants, answeredByUser, totalGroups, isManager))
                .groups(null)
                .callerAnsweredCount(answeredByUser.getOrDefault(callerId, 0L).intValue())
                .totalGroupCount(totalGroups)
                .build();
    }

    /**
     * Full detail DTO for a QA manager — all sessions and participant
     * identity fully visible.
     *
     * @param round          The round entity with groups and participants loaded.
     * @param groups         Groups with sessions loaded.
     * @param participants   All enrolled participants.
     * @param expertSessions All sessions submitted by the expert.
     * @param answeredByUser Map of userId → count of groups answered.
     */
    public CalibrationRoundResponseDTO toDTOForManager(CalibrationRound round,
                                                       List<CalibrationGroup> groups,
                                                       List<CalibrationParticipant> participants,
                                                       List<CalibrationSession> expertSessions,
                                                       Map<Integer, Long> answeredByUser) {
        boolean roundOpen = Boolean.TRUE.equals(round.getIsOpen());
        Map<Long, String> expertAnswerMap =
                groupMapper.buildExpertAnswerMap(groups, expertSessions);

        List<CalibrationGroupResponseDTO> groupDTOs = groups.stream()
                .map(g -> groupMapper.toDTOForManager(
                        g,
                        roundOpen ? null : expertAnswerMap.get(g.getGroupId())))
                .toList();

        return CalibrationRoundResponseDTO.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .clientId(round.getClient().getClientId())
                .clientName(round.getClient().getClientName())
                .protocolId(round.getProtocol().getProtocolId())
                .protocolName(round.getProtocol().getProtocolName())
                .questionId(round.getQuestion().getQuestionId())
                .questionText(round.getQuestion().getQuestionText())
                .category(round.getQuestion().getCategory().name())
                .isOpen(roundOpen)
                .isCalibrated(round.getIsCalibrated())
                .createdByName(round.getCreatedBy().getFullName())
                .createdAt(round.getCreatedAt())
                .groups(groupDTOs)
                .participants(toParticipantDTOs(participants, answeredByUser,
                        groups.size(), true))
                .callerAnsweredCount(null)
                .totalGroupCount(groups.size())
                .build();
    }

    /**
     * Full detail DTO for a participant — only their own sessions visible,
     * expert identity hidden.
     *
     * @param round          The round entity.
     * @param groups         Groups with sessions loaded.
     * @param participants   All enrolled participants.
     * @param expertSessions All sessions submitted by the expert.
     * @param answeredByUser Map of userId → count of groups answered.
     * @param callerId       The calling participant's user ID.
     */
    public CalibrationRoundResponseDTO toDTOForParticipant(CalibrationRound round,
                                                           List<CalibrationGroup> groups,
                                                           List<CalibrationParticipant> participants,
                                                           List<CalibrationSession> expertSessions,
                                                           Map<Integer, Long> answeredByUser,
                                                           Integer callerId) {
        boolean roundOpen = Boolean.TRUE.equals(round.getIsOpen());
        Map<Long, String> expertAnswerMap =
                groupMapper.buildExpertAnswerMap(groups, expertSessions);

        List<CalibrationGroupResponseDTO> groupDTOs = groups.stream()
                .map(g -> groupMapper.toDTOForParticipant(
                        g,
                        callerId,
                        expertAnswerMap.get(g.getGroupId()),
                        roundOpen))
                .toList();

        return CalibrationRoundResponseDTO.builder()
                .roundId(round.getRoundId())
                .roundName(round.getRoundName())
                .clientId(round.getClient().getClientId())
                .clientName(round.getClient().getClientName())
                .protocolId(round.getProtocol().getProtocolId())
                .protocolName(round.getProtocol().getProtocolName())
                .questionId(round.getQuestion().getQuestionId())
                .questionText(round.getQuestion().getQuestionText())
                .category(round.getQuestion().getCategory().name())
                .isOpen(roundOpen)
                .isCalibrated(round.getIsCalibrated())
                .createdByName(round.getCreatedBy().getFullName())
                .createdAt(round.getCreatedAt())
                .groups(groupDTOs)
                .participants(toParticipantDTOs(participants, answeredByUser,
                        groups.size(), false))
                .callerAnsweredCount(answeredByUser.getOrDefault(callerId, 0L).intValue())
                .totalGroupCount(groups.size())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private List<CalibrationParticipantResponseDTO> toParticipantDTOs(
            List<CalibrationParticipant> participants,
            Map<Integer, Long> answeredByUser,
            int totalGroups,
            boolean isManager) {

        return participants.stream()
                .map(p -> {
                    long answered = answeredByUser.getOrDefault(
                            p.getUser().getUserId(), 0L);
                    return CalibrationParticipantResponseDTO.builder()
                            .userId(p.getUser().getUserId())
                            .fullName(p.getUser().getFullName())
                            .roleName(p.getUser().getRole() != null
                                    ? p.getUser().getRole().getRoleName() : null)
                            // isExpert only visible to managers
                            .isExpert(isManager ? p.getIsExpert() : null)
                            .hasAnsweredAll(answered == totalGroups)
                            .answeredCount((int) answered)
                            .totalCount(totalGroups)
                            .build();
                })
                .toList();
    }
}