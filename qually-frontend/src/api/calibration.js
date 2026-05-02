/** @module api/calibration */

import { api } from "./apiClient";

/**
 * @typedef {Object} CalibrationSessionResponseDTO
 * @property {number}      calibrationSessionId
 * @property {number}      userId
 * @property {string}      userFullName
 * @property {string}      calibrationAnswer   - "YES", "NO", or "N/A"
 * @property {boolean|null} isCalibrated        - null until round closes
 * @property {string|null} expertAnswer         - null while open; own session only
 * @property {string}      answeredAt
 */

/**
 * @typedef {Object} CalibrationGroupResponseDTO
 * @property {number}      groupId
 * @property {string}      interactionId
 * @property {boolean|null} isCalibrated
 * @property {string|null} expertAnswer         - null while open
 * @property {CalibrationSessionResponseDTO[]} sessions
 */

/**
 * @typedef {Object} CalibrationParticipantResponseDTO
 * @property {number}      userId
 * @property {string}      fullName
 * @property {string}      roleName
 * @property {boolean|null} isExpert            - null for participants, true/false for managers
 * @property {boolean}     hasAnsweredAll
 * @property {number}      answeredCount
 * @property {number}      totalCount
 */

/**
 * @typedef {Object} CalibrationRoundResponseDTO
 * @property {number}      roundId
 * @property {string}      roundName
 * @property {number}      clientId
 * @property {string}      clientName
 * @property {number}      protocolId
 * @property {string}      protocolName
 * @property {number}      questionId
 * @property {string}      questionText
 * @property {string}      category
 * @property {boolean}     isOpen
 * @property {boolean|null} isCalibrated
 * @property {string}      createdByName
 * @property {string}      createdAt
 * @property {CalibrationGroupResponseDTO[]|null}       groups
 * @property {CalibrationParticipantResponseDTO[]|null} participants
 * @property {number|null} callerAnsweredCount
 * @property {number}      totalGroupCount
 */

/**
 * Creates a new calibration round.
 *
 * @param {{ clientId, protocolId, questionId, interactionIds, participantUserIds, expertUserId }} dto
 * @returns {Promise<CalibrationRoundResponseDTO>}
 */
export const createRound = (dto) =>
  api.post("/calibration/rounds", dto);

/**
 * Returns all rounds visible to the current user, scoped by role.
 *
 * @returns {Promise<CalibrationRoundResponseDTO[]>}
 */
export const getRounds = () =>
  api.get("/calibration/rounds");

/**
 * Returns full detail for a round with sessions and participant data.
 *
 * @param {number} id
 * @returns {Promise<CalibrationRoundResponseDTO>}
 */
export const getRoundDetail = (id) =>
  api.get(`/calibration/rounds/${id}`);

/**
 * Closes a round and triggers calibration comparison.
 * QA manager only.
 *
 * @param {number} id
 * @returns {Promise<CalibrationRoundResponseDTO>}
 */
export const closeAndCompare = (id) =>
  api.post(`/calibration/rounds/${id}/close`);

/**
 * Submits the current user's answer for one interaction group.
 *
 * @param {number} groupId
 * @param {string} calibrationAnswer - "YES", "NO", or "N/A"
 * @returns {Promise<CalibrationSessionResponseDTO>}
 */
export const submitAnswer = (groupId, calibrationAnswer) =>
  api.post(`/calibration/groups/${groupId}/answer`, { calibrationAnswer });