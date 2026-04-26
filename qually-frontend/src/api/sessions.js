/** @module api/sessions */

import { api } from "./apiClient";

/**
 * @typedef {Object} AuditSessionRequestDTO
 * @property {number}              protocolId
 * @property {string}              interactionId
 * @property {number}              auditorUserId
 * @property {number}              memberAuditedUserId
 * @property {number}              lobId
 * @property {string}              [comments]
 * @property {"DRAFT"|"COMPLETED"} [auditStatus]
 */

/**
 * @typedef {Object} AuditSessionResponseDTO
 * @property {number}                                        sessionId
 * @property {"DRAFT"|"COMPLETED"|"DISPUTED"|"RESOLVED"}    auditStatus
 * @property {string}                                        interactionId
 * @property {string}                                        [comments]
 * @property {number}                                        protocolId
 * @property {string}                                        protocolName
 * @property {number}                                        protocolVersion
 * @property {"STANDARD"|"ACCOUNTABILITY"}                   auditLogicType
 * @property {number}                                        clientId
 * @property {string}                                        clientName
 * @property {number|null}                                   auditorUserId
 * @property {string|null}                                   auditorName
 * @property {number|null}                                   memberAuditedUserId
 * @property {string|null}                                   memberAuditedName
 * @property {number|null}                                   lobId
 * @property {string|null}                                   lobName
 * @property {"UNCHANGED"|"MODIFIED"|null}                   resolutionOutcome
 * @property {string}                                        [startedAt]
 * @property {string}                                        [submittedAt]
 */

/**
 * @typedef {Object} ResumeResponseItemDTO
 * @property {number}   questionId
 * @property {string}   questionAnswer          - "YES", "NO", or "N/A".
 * @property {number[]} subattributeOptionIds   - Previously selected option IDs (may be empty).
 */

/**
 * @typedef {Object} SessionResumeDTO
 * @property {number}                  sessionId
 * @property {string}                  interactionId
 * @property {number|null}             lobId
 * @property {number|null}             memberAuditedUserId
 * @property {string|null}             comments
 * @property {ResumeResponseItemDTO[]} responses
 */

/**
 * @typedef {Object} SubattributeAnswerItemDTO
 * @property {number} subattributeOptionId
 */

/**
 * @typedef {Object} AuditResponseItemDTO
 * @property {number}   questionId
 * @property {string}   questionAnswer
 * @property {SubattributeAnswerItemDTO[]} [subattributeAnswers]
 */

/**
 * @typedef {Object} BulkAuditAnswerRequestDTO
 * @property {number}                 sessionId
 * @property {AuditResponseItemDTO[]} responses
 */

/**
 * @typedef {Object} AuditResponseDTO
 * @property {number} auditResponseId
 * @property {number} sessionId
 * @property {number} questionId
 * @property {string} questionText
 * @property {string} questionAnswer
 */

export const getSessions = ({ auditorUserId, auditStatus } = {}) => {
  const params = new URLSearchParams();
  if (auditorUserId != null) params.set("auditorUserId", auditorUserId);
  if (auditStatus)           params.set("auditStatus",   auditStatus);
  const qs = params.toString();
  return api.get(qs ? `/sessions?${qs}` : "/sessions");
};

/**
 * Fetches all DRAFT sessions visible to the current user.
 *
 * @returns {Promise<AuditSessionResponseDTO[]>}
 */
export const getDraftSessions = () =>
  api.get("/sessions?auditStatus=DRAFT");

export const getSessionById = (sessionId) =>
  api.get(`/sessions/${sessionId}`);

/**
 * Fetches the resume payload for a DRAFT session — metadata fields and
 * previously recorded answers including subattribute selections.
 *
 * @param {number} sessionId
 * @returns {Promise<SessionResumeDTO>}
 */
export const getSessionForResume = (sessionId) =>
  api.get(`/sessions/${sessionId}/resume`);

/**
 * Creates a new audit session.
 *
 * @param {AuditSessionRequestDTO} sessionData
 * @returns {Promise<AuditSessionResponseDTO>}
 */
export const createSession = (sessionData) =>
  api.post("/sessions", sessionData);

export const updateSession = (sessionId, data) =>
  api.put(`/sessions/${sessionId}`, data);

/**
 * Submits bulk answers for a session including subattribute selections.
 *
 * @param {BulkAuditAnswerRequestDTO} dto
 * @returns {Promise<AuditResponseDTO[]>}
 */
export const submitBulkResponses = (dto) =>
  api.post("/responses/bulk", dto);