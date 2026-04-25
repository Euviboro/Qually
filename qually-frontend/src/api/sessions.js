/** @module api/sessions */

import { api } from "./apiClient";

/**
 * @typedef {Object} AuditSessionRequestDTO
 * @property {number}                  protocolId
 * @property {string}                  interactionId
 * @property {number}                  auditorUserId
 * @property {number}                  memberAuditedUserId
 * @property {number}                  lobId
 * @property {string}                  [comments]
 * @property {"DRAFT"|"COMPLETED"}     [auditStatus]
 */

/**
 * @typedef {Object} AuditSessionResponseDTO
 * @property {number}                        sessionId
 * @property {"DRAFT"|"COMPLETED"|"DISPUTED"|"RESOLVED"} auditStatus
 * @property {string}                        interactionId
 * @property {string}                        memberAuditedName
 * @property {string}                        [comments]
 * @property {number}                        protocolId
 * @property {string}                        protocolName
 * @property {number}                        protocolVersion
 * @property {"STANDARD"|"ACCOUNTABILITY"}   auditLogicType
 * @property {number}                        clientId
 * @property {string}                        clientName
 * @property {number|null}                   auditorUserId
 * @property {string|null}                   auditorName
 * @property {number|null}                   memberAuditedUserId
 * @property {number|null}                   lobId
 * @property {string|null}                   lobName
 * @property {"UNCHANGED"|"MODIFIED"|null}   resolutionOutcome
 * @property {string}                        [startedAt]
 * @property {string}                        [submittedAt]
 */

/**
 * A single subattribute option selection within a bulk response item.
 *
 * @typedef {Object} SubattributeAnswerItemDTO
 * @property {number} subattributeOptionId - FK to subattribute_options.
 */

/**
 * A single question answer within a bulk response submission.
 *
 * @typedef {Object} AuditResponseItemDTO
 * @property {number}   questionId
 * @property {string}   questionAnswer      - "YES", "NO", or "N/A".
 * @property {SubattributeAnswerItemDTO[]} [subattributeAnswers]
 *   Option selections for sub-criteria. Only sent when questionAnswer is "NO"
 *   and the auditor made at least one selection. Omitted otherwise.
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

export const getSessionById = (sessionId) =>
  api.get(`/sessions/${sessionId}`);

/**
 * Creates a new audit session.
 * Omit {@code auditStatus} to default to DRAFT.
 *
 * @param {AuditSessionRequestDTO} sessionData
 * @returns {Promise<AuditSessionResponseDTO>}
 */
export const createSession = (sessionData) =>
  api.post("/sessions", sessionData);

export const updateSession = (sessionId, data) =>
  api.put(`/sessions/${sessionId}`, data);

/**
 * Submits bulk YES/NO/N/A answers for a session, including any subattribute
 * option selections made for NO answers.
 *
 * @param {BulkAuditAnswerRequestDTO} dto
 * @returns {Promise<AuditResponseDTO[]>}
 */
export const submitBulkResponses = (dto) =>
  api.post("/responses/bulk", dto);
