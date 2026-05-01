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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalibrationService {

    private static final Logger log = LoggerFactory.getLogger(CalibrationService.class);

    private static final DateTimeFormatter PERIOD_FORMAT =
            DateTimeFormatter.ofPattern("yyMM");

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

    @Transactional
    public CalibrationRoundResponseDTO createRound(CalibrationRoundRequestDTO dto,
                                                   Integer creatorUserId) {
        User creator = findUser(creatorUserId);
        validateIsQA(creator, "create calibration rounds");

        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(dto.getClientId())));
        AuditProtocol protocol = protocolRepository.findById(dto.getProtocolId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Protocol with ID %d not found".formatted(dto.getProtocolId())));
        AuditQuestion question = questionRepository.findById(dto.getQuestionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Question with ID %d not found".formatted(dto.getQuestionId())));

        if (!question.getAuditProtocol().getProtocolId().equals(protocol.getProtocolId())) {
            throw new IllegalArgumentException(
                    "Question %d does not belong to protocol %d"
                            .formatted(dto.getQuestionId(), dto.getProtocolId()));
        }

        List<String> interactionIds = dto.getInteractionIds();
        Set<String> uniqueIds = new HashSet<>(interactionIds);
        if (uniqueIds.size() != interactionIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate interaction IDs in request — each must be unique within a round.");
        }

        List<Integer> participantIds = dto.getParticipantUserIds();
        if (!participantIds.contains(dto.getExpertUserId())) {
            throw new IllegalArgumentException(
                    "Expert user ID %d must be included in participantUserIds."
                            .formatted(dto.getExpertUserId()));
        }

        // Generate round name — throws if abbreviations are missing
        String roundName = generateRoundName(client, protocol,
                question.getCategory().name(), LocalDateTime.now());

        // Ensure creator is enrolled
        Set<Integer> allParticipantIds = new LinkedHashSet<>(participantIds);
        allParticipantIds.add(creatorUserId);

        Map<Integer, User> userMap = new HashMap<>();
        for (Integer userId : allParticipantIds) {
            userMap.put(userId, findUser(userId));
        }

        CalibrationRound round = roundRepository.save(
                CalibrationRound.builder()
                        .roundName(roundName)
                        .client(client)
                        .protocol(protocol)
                        .question(question)
                        .createdBy(creator)
                        .isOpen(true)
                        .build());

        List<CalibrationGroup> groups = interactionIds.stream()
                .map(id -> groupRepository.save(
                        CalibrationGroup.builder()
                                .round(round)
                                .interactionId(id)
                                .build()))
                .toList();

        List<CalibrationParticipant> participants = allParticipantIds.stream()
                .map(uid -> participantRepository.save(
                        CalibrationParticipant.builder()
                                .round(round)
                                .user(userMap.get(uid))
                                .isExpert(uid.equals(dto.getExpertUserId()))
                                .build()))
                .toList();

        log.info("Calibration round {} '{}' created by user {} — {} interactions, {} participants",
                round.getRoundId(), roundName, creatorUserId,
                groups.size(), participants.size());

        return roundMapper.toSummaryDTO(round, participants, Map.of(),
                groups.size(), creatorUserId, false);
    }

    // ── Submit answer ─────────────────────────────────────────

    @Transactional
    public CalibrationSessionResponseDTO submitAnswer(Long groupId,
                                                      String answer,
                                                      Integer userId) {
        AuditAnswerType.fromValue(answer);

        CalibrationGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Calibration group with ID %d not found".formatted(groupId)));

        CalibrationRound round = group.getRound();

        if (!Boolean.TRUE.equals(round.getIsOpen())) {
            throw new IllegalStateException(
                    "This calibration round is closed — no more answers can be submitted.");
        }

        participantRepository.findByRound_RoundIdAndUser_UserId(round.getRoundId(), userId)
                .orElseThrow(() -> new IllegalStateException(
                        "You are not enrolled in this calibration round."));

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

    @Transactional
    public CalibrationRoundResponseDTO closeAndCompare(Long roundId, Integer closerUserId) {
        CalibrationRound round = findRoundOrThrow(roundId);
        User closer = findUser(closerUserId);

        validateIsQA(closer, "close calibration rounds");

        if (!Boolean.TRUE.equals(round.getIsOpen())) {
            throw new IllegalStateException("This calibration round is already closed.");
        }

        CalibrationParticipant expertParticipant =
                participantRepository.findByRound_RoundIdAndIsExpertTrue(roundId)
                        .orElseThrow(() -> new IllegalStateException(
                                "No expert is assigned to this round."));
        Integer expertUserId = expertParticipant.getUser().getUserId();

        List<CalibrationGroup> groups = groupRepository.findByRoundIdWithSessions(roundId);

        Map<Long, String> expertAnswerMap = new HashMap<>();
        for (CalibrationGroup group : groups) {
            group.getSessions().stream()
                    .filter(s -> s.getUser().getUserId().equals(expertUserId))
                    .findFirst()
                    .ifPresent(s -> expertAnswerMap.put(group.getGroupId(),
                            s.getCalibrationAnswer()));
        }

        boolean allGroupsCalibrated = true;

        for (CalibrationGroup group : groups) {
            String expertAnswer = expertAnswerMap.get(group.getGroupId());

            if (expertAnswer == null) {
                group.setIsCalibrated(false);
                allGroupsCalibrated = false;
                log.warn("Expert has not answered group {} — group marked not calibrated",
                        group.getGroupId());
                groupRepository.save(group);
                continue;
            }

            boolean allMatch = true;
            for (CalibrationSession session : group.getSessions()) {
                boolean isExpert = session.getUser().getUserId().equals(expertUserId);
                boolean matches  = isExpert || expertAnswer.equals(session.getCalibrationAnswer());
                session.setIsCalibrated(matches);
                sessionRepository.save(session);
                if (!matches) allMatch = false;
            }

            group.setIsCalibrated(allMatch);
            groupRepository.save(group);
            if (!allMatch) allGroupsCalibrated = false;
        }

        round.setIsCalibrated(allGroupsCalibrated);
        round.setIsOpen(false);
        roundRepository.save(round);

        log.info("Calibration round {} '{}' closed by user {} — result: {}",
                roundId, round.getRoundName(), closerUserId,
                allGroupsCalibrated ? "CALIBRATED" : "NOT CALIBRATED");

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

    @Transactional(readOnly = true)
    public List<CalibrationRoundResponseDTO> getRounds(Integer userId) {
        User user = findUser(userId);

        if (!Department.QA.equals(getDepartment(user))) {
            return List.of();
        }

        boolean isManager = isQaManager(userId);
        List<CalibrationRound> rounds;

        if (isManager) {
            List<Integer> subordinateIds = userRepository.findAllSubordinateIds(userId);
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

        return rounds.stream().map(r -> {
            List<CalibrationParticipant> participants =
                    participantRepository.findByRound_RoundId(r.getRoundId());
            int totalGroups = groupRepository.findByRound_RoundId(r.getRoundId()).size();
            return roundMapper.toSummaryDTO(r, participants, Map.of(),
                    totalGroups, userId, isManager);
        }).toList();
    }

    // ── Get round detail ──────────────────────────────────────

    @Transactional(readOnly = true)
    public CalibrationRoundResponseDTO getRoundDetail(Long roundId, Integer userId) {
        CalibrationRound round = roundRepository.findByIdWithDetails(roundId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Calibration round with ID %d not found".formatted(roundId)));

        User user = findUser(userId);
        boolean isManager = isQaManager(userId);

        List<CalibrationGroup> groups =
                groupRepository.findByRoundIdWithSessions(roundId);
        List<CalibrationParticipant> participants =
                participantRepository.findByRound_RoundId(roundId);

        CalibrationParticipant expertParticipant = participants.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsExpert()))
                .findFirst().orElse(null);
        Integer expertUserId = expertParticipant != null
                ? expertParticipant.getUser().getUserId() : null;

        List<CalibrationSession> expertSessions = expertUserId != null
                ? sessionRepository.findByRoundIdAndUserId(roundId, expertUserId)
                : List.of();

        Map<Integer, Long> answeredByUser = buildAnsweredByUserMap(roundId);

        if (isManager) {
            return roundMapper.toDTOForManager(round, groups, participants,
                    expertSessions, answeredByUser);
        }

        boolean isEnrolled = participants.stream()
                .anyMatch(p -> p.getUser().getUserId().equals(userId));
        if (!isEnrolled) {
            throw new IllegalStateException(
                    "You are not enrolled in this calibration round.");
        }
        return roundMapper.toDTOForParticipant(round, groups, participants,
                expertSessions, answeredByUser, userId);
    }

    // ── Round name generation ─────────────────────────────────

    /**
     * Generates an auto-formatted round name.
     * Format: {@code {clientAbbr}-{protocolAbbr}-{CAT}-{YYMM}-{SEQ}}
     * Example: {@code DSV-DSP-BUS-2604-001}
     *
     * <p>The sequence number is 1-based within the same
     * client + protocol + category + period combination.</p>
     *
     * @throws IllegalArgumentException if either abbreviation is missing.
     */
    private String generateRoundName(Client client, AuditProtocol protocol,
                                     String category, LocalDateTime now) {
        if (client.getClientAbbreviation() == null
                || client.getClientAbbreviation().isBlank()) {
            throw new IllegalArgumentException(
                    "Client '%s' has no abbreviation set. Add it in Settings before creating a calibration round."
                            .formatted(client.getClientName()));
        }
        if (protocol.getProtocolAbbreviation() == null
                || protocol.getProtocolAbbreviation().isBlank()) {
            throw new IllegalArgumentException(
                    "Protocol '%s' has no abbreviation set. Add it to the protocol before creating a calibration round."
                            .formatted(protocol.getProtocolName()));
        }

        String catCode = switch (category) {
            case "CUSTOMER"   -> "CUS";
            case "BUSINESS"   -> "BUS";
            case "COMPLIANCE" -> "COM";
            default           -> category.length() >= 3
                    ? category.substring(0, 3).toUpperCase()
                    : category.toUpperCase();
        };

        String period = now.format(PERIOD_FORMAT); // e.g. "2604"

        long existing = roundRepository.countByClientAndProtocolAndCategoryAndPeriod(
                client.getClientId(),
                protocol.getProtocolId(),
                category,
                period);

        return "%s-%s-%s-%s-%03d".formatted(
                client.getClientAbbreviation().toUpperCase(),
                protocol.getProtocolAbbreviation().toUpperCase(),
                catCode,
                period,
                existing + 1);
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
            throw new IllegalStateException("Only QA users can %s.".formatted(action));
        }
    }

    private Department getDepartment(User user) {
        return user.getRole() != null ? user.getRole().getDepartment() : null;
    }

    private boolean isQaManager(Integer userId) {
        List<Integer> subordinates = userRepository.findAllSubordinateIds(userId);
        return !subordinates.isEmpty();
    }

    private Map<Integer, Long> buildAnsweredByUserMap(Long roundId) {
        return sessionRepository.findByRoundId(roundId).stream()
                .collect(Collectors.groupingBy(
                        s -> s.getUser().getUserId(),
                        Collectors.counting()));
    }
}