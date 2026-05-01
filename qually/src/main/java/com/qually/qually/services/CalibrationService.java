package com.qually.qually.services;

import com.qually.qually.dto.request.CalibrationRoundRequestDTO;
import com.qually.qually.dto.response.CalibrationRoundResponseDTO;
import com.qually.qually.dto.response.CalibrationSessionResponseDTO;
import com.qually.qually.mappers.CalibrationGroupMapper;
import com.qually.qually.mappers.CalibrationRoundMapper;
import com.qually.qually.mappers.CalibrationSessionMapper;
import com.qually.qually.models.*;
import com.qually.qually.models.enums.AuditAnswerType;
import com.qually.qually.models.enums.Department;
import com.qually.qually.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CalibrationService {

    private static final Logger log = LoggerFactory.getLogger(CalibrationService.class);

    private final CalibrationRoundRepository       roundRepository;
    private final CalibrationGroupRepository       groupRepository;
    private final CalibrationParticipantRepository participantRepository;
    private final CalibrationSessionRepository     sessionRepository;
    private final UserRepository                   userRepository;
    private final AuditProtocolRepository          protocolRepository;
    private final AuditQuestionRepository          questionRepository;
    private final ClientRepository                 clientRepository;
    private final CalibrationRoundMapper           roundMapper;
    private final CalibrationGroupMapper           groupMapper;
    private final CalibrationSessionMapper         sessionMapper;

    public CalibrationService(
            CalibrationRoundRepository       roundRepository,
            CalibrationGroupRepository       groupRepository,
            CalibrationParticipantRepository participantRepository,
            CalibrationSessionRepository     sessionRepository,
            UserRepository                   userRepository,
            AuditProtocolRepository          protocolRepository,
            AuditQuestionRepository          questionRepository,
            ClientRepository                 clientRepository,
            CalibrationRoundMapper           roundMapper,
            CalibrationGroupMapper           groupMapper,
            CalibrationSessionMapper         sessionMapper) {
        this.roundRepository       = roundRepository;
        this.groupRepository       = groupRepository;
        this.participantRepository = participantRepository;
        this.sessionRepository     = sessionRepository;
        this.userRepository        = userRepository;
        this.protocolRepository    = protocolRepository;
        this.questionRepository    = questionRepository;
        this.clientRepository      = clientRepository;
        this.roundMapper           = roundMapper;
        this.groupMapper           = groupMapper;
        this.sessionMapper         = sessionMapper;
    }

    // ── Create round ──────────────────────────────────────────

    /**
     * Creates a new calibration round with its groups and participants.
     * The creator is automatically enrolled as a participant even if
     * not listed in {@code participantUserIds}.
     *
     * @throws IllegalArgumentException if validation fails.
     * @throws IllegalStateException if the caller is not a QA user.
     */
    @Transactional
    public CalibrationRoundResponseDTO createRound(CalibrationRoundRequestDTO dto,
                                                   Integer creatorUserId) {
        User creator = findUser(creatorUserId);
        validateIsQA(creator, "create calibration rounds");

        Client        client   = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(dto.getClientId())));
        AuditProtocol protocol = protocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(dto.getProtocolId())));
        AuditQuestion question = questionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Question with ID %d not found".formatted(dto.getQuestionId())));

        // Validate question belongs to protocol
        if (!question.getAuditProtocol().getProtocolId().equals(protocol.getProtocolId())) {
            throw new IllegalArgumentException(
                    "Question %d does not belong to protocol %d"
                            .formatted(dto.getQuestionId(), dto.getProtocolId()));
        }

        // Validate interaction IDs are unique within the request
        List<String> interactionIds = dto.getInteractionIds();
        Set<String> uniqueIds = new HashSet<>(interactionIds);
        if (uniqueIds.size() != interactionIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate interaction IDs in request — each must be unique within a round.");
        }

        // Validate expert is in participant list
        List<Integer> participantIds = dto.getParticipantUserIds();
        if (!participantIds.contains(dto.getExpertUserId())) {
            throw new IllegalArgumentException(
                    "Expert user ID %d must be included in participantUserIds."
                            .formatted(dto.getExpertUserId()));
        }

        // Ensure creator is enrolled even if omitted from the list
        Set<Integer> allParticipantIds = new LinkedHashSet<>(participantIds);
        allParticipantIds.add(creatorUserId);

        // Resolve participant users
        Map<Integer, User> userMap = new HashMap<>();
        for (Integer userId : allParticipantIds) {
            userMap.put(userId, findUser(userId));
        }

        // Save round
        CalibrationRound round = roundRepository.save(
                CalibrationRound.builder()
                        .roundName(dto.getRoundName())
                        .client(client)
                        .protocol(protocol)
                        .question(question)
                        .createdBy(creator)
                        .isOpen(true)
                        .build());

        // Save groups (one per interaction ID)
        List<CalibrationGroup> groups = interactionIds.stream()
                .map(id -> groupRepository.save(
                        CalibrationGroup.builder()
                                .round(round)
                                .interactionId(id)
                                .build()))
                .toList();

        // Save participants
        List<CalibrationParticipant> participants = allParticipantIds.stream()
                .map(uid -> participantRepository.save(
                        CalibrationParticipant.builder()
                                .round(round)
                                .user(userMap.get(uid))
                                .isExpert(uid.equals(dto.getExpertUserId()))
                                .build()))
                .toList();

        log.info("Calibration round {} '{}' created by user {} — {} interactions, {} participants",
                round.getRoundId(), round.getRoundName(), creatorUserId,
                groups.size(), participants.size());

        Map<Integer, Long> answeredByUser = Map.of();
        return roundMapper.toSummaryDTO(round, participants, answeredByUser,
                groups.size(), creatorUserId, false);
    }

    // ── Submit answer ─────────────────────────────────────────

    /**
     * Submits a participant's answer for one interaction group.
     * Answers are immutable — submitting twice returns 400.
     *
     * @return The saved session as a DTO with no expert answer
     *         (round is still open).
     */
    @Transactional
    public CalibrationSessionResponseDTO submitAnswer(Long groupId,
                                                      String answer,
                                                      Integer userId) {
        // Validate answer value
        AuditAnswerType.fromValue(answer);

        CalibrationGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Calibration group with ID %d not found".formatted(groupId)));

        CalibrationRound round = group.getRound();

        if (!Boolean.TRUE.equals(round.getIsOpen())) {
            throw new IllegalStateException(
                    "This calibration round is closed — no more answers can be submitted.");
        }

        // Validate user is enrolled
        participantRepository.findByRound_RoundIdAndUser_UserId(round.getRoundId(), userId)
                .orElseThrow(() -> new IllegalStateException(
                        "You are not enrolled in this calibration round."));

        // Enforce no-update rule
        if (sessionRepository.findByGroup_GroupIdAndUser_UserId(groupId, userId).isPresent()) {
            throw new IllegalArgumentException(
                    "You have already submitted an answer for interaction '%s'."
                            .formatted(group.getInteractionId()));
        }

        User user = findUser(userId);
        CalibrationSession session = sessionRepository.save(
                CalibrationSession.builder()
                        .group(group)
                        .user(user)
                        .calibrationAnswer(answer)
                        .build());

        log.info("User {} answered '{}' for group {} (interaction '{}')",
                userId, answer, groupId, group.getInteractionId());

        return sessionMapper.toDTO(session, null);
    }

    // ── Close and compare ─────────────────────────────────────

    /**
     * Closes a round and compares all participant answers against the expert's.
     *
     * <p>Comparison logic:</p>
     * <ol>
     *   <li>For each group, find the expert's answer.</li>
     *   <li>For every other session: {@code isCalibrated = (answer == expertAnswer)}.</li>
     *   <li>The expert's own session: always {@code isCalibrated = true}.</li>
     *   <li>Group {@code isCalibrated = true} only if all sessions match.</li>
     *   <li>Round {@code isCalibrated = true} only if all groups pass.</li>
     * </ol>
     *
     * <p>Only a QA user at or above the creator's manager level can close
     * a round.</p>
     */
    @Transactional
    public CalibrationRoundResponseDTO closeAndCompare(Long roundId, Integer closerUserId) {
        CalibrationRound round = findRoundOrThrow(roundId);
        User closer = findUser(closerUserId);

        validateIsQA(closer, "close calibration rounds");

        if (!Boolean.TRUE.equals(round.getIsOpen())) {
            throw new IllegalStateException("This calibration round is already closed.");
        }

        // Find the expert
        CalibrationParticipant expertParticipant =
                participantRepository.findByRound_RoundIdAndIsExpertTrue(roundId)
                        .orElseThrow(() -> new IllegalStateException(
                                "No expert is assigned to this round."));
        Integer expertUserId = expertParticipant.getUser().getUserId();

        // Load all groups with their sessions
        List<CalibrationGroup> groups =
                groupRepository.findByRoundIdWithSessions(roundId);

        // Build expert answer map: groupId → expert's answer
        Map<Long, String> expertAnswerMap = new HashMap<>();
        for (CalibrationGroup group : groups) {
            group.getSessions().stream()
                    .filter(s -> s.getUser().getUserId().equals(expertUserId))
                    .findFirst()
                    .ifPresent(s -> expertAnswerMap.put(group.getGroupId(),
                            s.getCalibrationAnswer()));
        }

        boolean allGroupsCalibrated = true;

        // Compare and persist results per group
        for (CalibrationGroup group : groups) {
            String expertAnswer = expertAnswerMap.get(group.getGroupId());

            if (expertAnswer == null) {
                // Expert has not answered this interaction — cannot calibrate
                group.setIsCalibrated(false);
                allGroupsCalibrated = false;
                log.warn("Expert has not answered group {} — group marked not calibrated",
                        group.getGroupId());
                groupRepository.save(group);
                continue;
            }

            boolean allMatch = true;
            for (CalibrationSession session : group.getSessions()) {
                boolean isExpertSession =
                        session.getUser().getUserId().equals(expertUserId);
                boolean matches = isExpertSession ||
                        expertAnswer.equals(session.getCalibrationAnswer());
                session.setIsCalibrated(matches);
                sessionRepository.save(session);
                if (!matches) allMatch = false;
            }

            group.setIsCalibrated(allMatch);
            groupRepository.save(group);

            if (!allMatch) allGroupsCalibrated = false;
        }

        // Update round
        round.setIsCalibrated(allGroupsCalibrated);
        round.setIsOpen(false);
        roundRepository.save(round);

        log.info("Calibration round {} '{}' closed by user {} — result: {}",
                roundId, round.getRoundName(), closerUserId,
                allGroupsCalibrated ? "CALIBRATED" : "NOT CALIBRATED");

        // Build full manager response
        List<CalibrationParticipant> participants =
                participantRepository.findByRound_RoundId(roundId);
        List<CalibrationSession> expertSessions = groups.stream()
                .flatMap(g -> g.getSessions().stream())
                .filter(s -> s.getUser().getUserId().equals(expertUserId))
                .toList();
        Map<Integer, Long> answeredByUser = buildAnsweredByUserMap(roundId);

        return roundMapper.toDTOForManager(round, groups, participants,
                expertSessions, answeredByUser);
    }

    // ── Get rounds ────────────────────────────────────────────

    /**
     * Returns all rounds visible to the calling user.
     *
     * <p>Visibility tiers:</p>
     * <ul>
     *   <li>Team Member → empty list (not eligible for calibration)</li>
     *   <li>QA Specialist → rounds they are enrolled in</li>
     *   <li>QA Manager+ → all rounds in their management chain</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public List<CalibrationRoundResponseDTO> getRounds(Integer userId) {
        User user = findUser(userId);

        if (!Department.QA.equals(getDepartment(user))) {
            return List.of();
        }

        boolean isManager = isQaManager(user, userId);
        List<CalibrationRound> rounds;

        if (isManager) {
            List<Integer> subordinateIds = userRepository.findAllSubordinateIds(userId);
            // Manager sees all rounds created by anyone in their chain
            // plus rounds they are enrolled in
            rounds = roundRepository.findAllWithDetails().stream()
                    .filter(r -> {
                        Integer creatorId = r.getCreatedBy().getUserId();
                        return creatorId.equals(userId) ||
                                subordinateIds.contains(creatorId);
                    })
                    .toList();
        } else {
            rounds = roundRepository.findByParticipantUserId(userId);
        }

        Map<Integer, Long> answeredByUser = new HashMap<>();

        return rounds.stream().map(r -> {
            List<CalibrationParticipant> participants =
                    participantRepository.findByRound_RoundId(r.getRoundId());
            int totalGroups = (int) groupRepository.findByRound_RoundId(r.getRoundId()).stream().count();
            return roundMapper.toSummaryDTO(r, participants, answeredByUser,
                    totalGroups, userId, isManager);
        }).toList();
    }

    // ── Get round detail ──────────────────────────────────────

    /**
     * Returns full detail for a round, applying visibility rules based
     * on the caller's role.
     */
    @Transactional(readOnly = true)
    public CalibrationRoundResponseDTO getRoundDetail(Long roundId, Integer userId) {
        CalibrationRound round = roundRepository.findByIdWithDetails(roundId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Calibration round with ID %d not found".formatted(roundId)));

        User user = findUser(userId);
        boolean isManager = isQaManager(user, userId);

        List<CalibrationGroup> groups =
                groupRepository.findByRoundIdWithSessions(roundId);
        List<CalibrationParticipant> participants =
                participantRepository.findByRound_RoundId(roundId);

        // Find expert
        CalibrationParticipant expertParticipant = participants.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsExpert()))
                .findFirst()
                .orElse(null);
        Integer expertUserId = expertParticipant != null
                ? expertParticipant.getUser().getUserId() : null;

        List<CalibrationSession> expertSessions = expertUserId != null
                ? sessionRepository.findByRoundIdAndUserId(roundId, expertUserId)
                : List.of();

        Map<Integer, Long> answeredByUser = buildAnsweredByUserMap(roundId);

        if (isManager) {
            return roundMapper.toDTOForManager(round, groups, participants,
                    expertSessions, answeredByUser);
        } else {
            // Verify caller is enrolled
            boolean isEnrolled = participants.stream()
                    .anyMatch(p -> p.getUser().getUserId().equals(userId));
            if (!isEnrolled) {
                throw new IllegalStateException(
                        "You are not enrolled in this calibration round.");
            }
            return roundMapper.toDTOForParticipant(round, groups, participants,
                    expertSessions, answeredByUser, userId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private CalibrationRound findRoundOrThrow(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Calibration round with ID %d not found".formatted(roundId)));
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(userId)));
    }

    private void validateIsQA(User user, String action) {
        if (!Department.QA.equals(getDepartment(user))) {
            throw new IllegalStateException(
                    "Only QA users can %s.".formatted(action));
        }
    }

    private Department getDepartment(User user) {
        return user.getRole() != null ? user.getRole().getDepartment() : null;
    }

    /**
     * A QA manager is a QA user who has at least one subordinate.
     * Uses the recursive CTE to check the management chain.
     */
    private boolean isQaManager(User user, Integer userId) {
        if (!Department.QA.equals(getDepartment(user))) return false;
        List<Integer> subordinates = userRepository.findAllSubordinateIds(userId);
        return !subordinates.isEmpty();
    }

    /**
     * Builds a map of userId → number of groups answered in a round.
     * Used by mappers to compute progress indicators.
     */
    private Map<Integer, Long> buildAnsweredByUserMap(Long roundId) {
        return sessionRepository.findByRoundId(roundId).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getUser().getUserId(),
                        Collectors.counting()));
    }
}