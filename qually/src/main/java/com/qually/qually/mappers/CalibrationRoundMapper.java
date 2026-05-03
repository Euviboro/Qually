package com.qually.qually.mappers;

import com.qually.qually.dto.response.CalibrationGroupResponseDTO;
import com.qually.qually.dto.response.CalibrationParticipantResponseDTO;
import com.qually.qually.dto.response.CalibrationRoundResponseDTO;
import com.qually.qually.models.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Maps {@link CalibrationRound} entities to {@link CalibrationRoundResponseDTO}.
 *
 * <p>Mapping paths:</p>
 * <ul>
 *   <li>{@link #toSummaryDTO}        — list view, no session detail.</li>
 *   <li>{@link #toDTOForManager}     — SR_QA full view. If also a participant,
 *       caller's own sessions are included via participant path.</li>
 *   <li>{@link #toDTOForParticipant} — PARTICIPANT, EXPERT, and CREATOR.
 *       Creator receives the same data but the frontend uses {@code callerRole}
 *       to additionally render the participant completion list.</li>
 * </ul>
 */
@Component
public class CalibrationRoundMapper {

    private final CalibrationGroupMapper groupMapper;

    public CalibrationRoundMapper(CalibrationGroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

    // ── List view ─────────────────────────────────────────────

    /**
     * Summary DTO for the rounds list — no group or session detail.
     *
     * @param callerRole     SR_QA / CREATOR / EXPERT / PARTICIPANT
     * @param isManager      Whether to expose isExpert on participants
     *                       (true for SR_QA).
     */
    public CalibrationRoundResponseDTO toSummaryDTO(CalibrationRound round,
                                                    List<CalibrationParticipant> participants,
                                                    Map<Integer, Long> answeredByUser,
                                                    int totalGroups,
                                                    Integer callerId,
                                                    String callerRole,
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
                .callerRole(callerRole)
                .isManagerParticipant(false)
                .participants(toParticipantDTOs(participants, answeredByUser,
                        totalGroups, isManager))
                .groups(null)
                .callerAnsweredCount(answeredByUser.getOrDefault(callerId, 0L).intValue())
                .totalGroupCount(totalGroups)
                .build();
    }

    // ── Detail: SR_QA ─────────────────────────────────────────

    /**
     * Full detail DTO for SR_QA. All participants' answers visible, expert
     * identified.
     *
     * <p>When the SR_QA is also a participant ({@code isManagerParticipant=true}),
     * the frontend renders a participant section above the manager section.
     * The participant section is built from this same response — the frontend
     * filters by {@code userId === caller.userId} in the sessions lists.</p>
     *
     * @param isManagerParticipant true when caller is both SR_QA and enrolled
     * @param callerId             the SR_QA's userId — needed for callerAnsweredCount
     */
    public CalibrationRoundResponseDTO toDTOForManager(CalibrationRound round,
                                                       List<CalibrationGroup> groups,
                                                       List<CalibrationParticipant> participants,
                                                       List<CalibrationSession> expertSessions,
                                                       Map<Integer, Long> answeredByUser,
                                                       boolean isManagerParticipant,
                                                       Integer callerId) {
        boolean roundOpen = Boolean.TRUE.equals(round.getIsOpen());
        Map<Long, String> expertAnswerMap =
                groupMapper.buildExpertAnswerMap(groups, expertSessions);

        List<CalibrationGroupResponseDTO> groupDTOs = groups.stream()
                .map(g -> groupMapper.toDTOForManager(
                        g, roundOpen ? null : expertAnswerMap.get(g.getGroupId())))
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
                .callerRole("SR_QA")
                .isManagerParticipant(isManagerParticipant)
                .groups(groupDTOs)
                .participants(toParticipantDTOs(participants, answeredByUser,
                        groups.size(), true))
                .callerAnsweredCount(answeredByUser.getOrDefault(callerId, 0L).intValue())
                .totalGroupCount(groups.size())
                .build();
    }

    // ── Detail: CREATOR / EXPERT / PARTICIPANT ────────────────

    /**
     * Detail DTO for CREATOR, EXPERT, and PARTICIPANT.
     *
     * <p>Groups show only the caller's own session. Expert answer revealed
     * after round closes. Participants list included with completion progress
     * but without isExpert — the frontend uses {@code callerRole} to decide
     * whether to render the participants section (CREATOR sees it,
     * PARTICIPANT and EXPERT do not).</p>
     *
     * @param callerRole CREATOR / EXPERT / PARTICIPANT
     */
    public CalibrationRoundResponseDTO toDTOForParticipant(CalibrationRound round,
                                                           List<CalibrationGroup> groups,
                                                           List<CalibrationParticipant> participants,
                                                           List<CalibrationSession> expertSessions,
                                                           Map<Integer, Long> answeredByUser,
                                                           Integer callerId,
                                                           String callerRole) {
        boolean roundOpen = Boolean.TRUE.equals(round.getIsOpen());
        Map<Long, String> expertAnswerMap =
                groupMapper.buildExpertAnswerMap(groups, expertSessions);

        List<CalibrationGroupResponseDTO> groupDTOs = groups.stream()
                .map(g -> groupMapper.toDTOForParticipant(
                        g, callerId, expertAnswerMap.get(g.getGroupId()), roundOpen))
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
                .callerRole(callerRole)
                .isManagerParticipant(false)
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
                            .isExpert(isManager ? p.getIsExpert() : null)
                            .hasAnsweredAll(answered == totalGroups)
                            .answeredCount((int) answered)
                            .totalCount(totalGroups)
                            .build();
                })
                .toList();
    }
}