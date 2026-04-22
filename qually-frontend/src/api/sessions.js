/** @module api/sessions */

import { api } from "./apiClient";

/**
 * @typedef {Object} AuditSessionRequestDTO
 * @property {number}                  protocolId
 * @property {string}                  interactionId    - External reference (e.g. call ID).
 * @property {number}                  auditorUserId    - Integer PK of the conducting user.
 * @property {string}                  memberAudited    - Name/ID of the person being audited.
 * @property {string}                  [comments]
 * @property {"DRAFT"|"COMPLETED"}     [auditStatus]    - Defaults to DRAFT on the server.
 */

/**
 * @typedef {Object} AuditSessionResponseDTO
 * @property {number}                        sessionId
 * @property {"DRAFT"|"COMPLETED"|"DISPUTED"|"RESOLVED"} auditStatus
 * @property {string}                        interactionId
 * @property {string}                        memberAudited
 * @property {string}                        [comments]
 * @property {number}                        protocolId
 * @property {string}                        protocolName
 * @property {number}                        protocolVersion
 * @property {"STANDARD"|"ACCOUNTABILITY"}   auditLogicType - From the protocol.
 * @property {number|null}                   auditorUserId
 * @property {string|null}                   auditorName
 * @property {"UNCHANGED"|"MODIFIED"|null}   resolutionOutcome
 * @property {string}                        [startedAt]
 * @property {string}                        [submittedAt]
 */

/**
 * @typedef {Object} AuditResponseItemDTO
 * @property {number} questionId
 * @property {string} questionAnswer - `"YES"` or `"NO"`.
 */

/**
 * @typedef {Object} BulkAuditAnswerRequestDTO
 * @property {number}                 sessionId
 * @property {AuditResponseItemDTO[]} responses
 */

/**
 * Fetches all sessions, optionally filtered by auditor user ID or status.
 *
 * @param {{ auditorUserId?: number, auditStatus?: string }} [params]
 * @returns {Promise<AuditSessionResponseDTO[]>}
 */
export const getSessions = ({ auditorUserId, auditStatus } = {}) => {
  const params = new URLSearchParams();
  if (auditorUserId != null) params.set("auditorUserId", auditorUserId);
  if (auditStatus)           params.set("auditStatus",   auditStatus);
  const qs = params.toString();
  return api.get(qs ? `/sessions?${qs}` : "/sessions");
};

/**
 * Fetches a single session by ID.
 *
 * @param {number|string} sessionId
 * @returns {Promise<AuditSessionResponseDTO>}
 */
export const getSessionById = (sessionId) =>
  api.get(`/sessions/${sessionId}`);

/**
 * Creates a new audit session.
 * Omit `auditStatus` to default to DRAFT, or pass `"COMPLETED"` to submit immediately.
 *
 * @param {AuditSessionRequestDTO} sessionData
 * @returns {Promise<AuditSessionResponseDTO>}
 */
export const createSession = (sessionData) =>
  api.post("/sessions", sessionData);

/**
 * Partially updates an existing session (status, comments).
 *
 * @param {number|string} sessionId
 * @param {{ auditStatus?: string, comments?: string }} data
 * @returns {Promise<AuditSessionResponseDTO>}
 */
export const updateSession = (sessionId, data) =>
  api.put(`/sessions/${sessionId}`, data);

/**
 * Submits bulk YES/NO answers for a session.
 *
 * @param {BulkAuditAnswerRequestDTO} dto
 * @returns {Promise<import('./questions').AuditResponseDTO[]>}
 */
export const submitBulkResponses = (dto) =>
  api.post("/responses/bulk", dto);
