/** @module api/disputes */

import { api } from "./apiClient";

/**
 * @typedef {Object} AuditDisputeRequestDTO
 * @property {number} responseId
 * @property {number} reasonId
 * @property {string} [disputeComment]
 */

/**
 * @typedef {Object} ResolveDisputeRequestDTO
 * @property {"UNCHANGED"|"MODIFIED"} resolutionOutcome
 * @property {string} [newAnswer] - Required when MODIFIED.
 * @property {string} [resolutionNote]
 */

/**
 * @typedef {Object} DisputeInboxRowDTO
 * @property {number}      sessionId
 * @property {string}      sessionDate
 * @property {string}      clientName
 * @property {string}      protocolName
 * @property {string|null} lobName
 * @property {string}      interactionId
 * @property {string|null} memberAuditedName
 * @property {number}      responseId
 * @property {string}      questionText
 * @property {string}      category
 * @property {string}      originalAnswer
 * @property {string}      effectiveAnswer
 * @property {string}      responseStatus
 * @property {boolean}     isFlagged
 * @property {"FLAGGED"|"DISPUTED"|"RESOLVED"} displayStatus
 * @property {number|null} disputeId
 * @property {string|null} reasonText
 * @property {string|null} disputeComment
 * @property {string|null} raisedByName
 * @property {string|null} raisedAt
 * @property {"UNCHANGED"|"MODIFIED"|null} resolutionOutcome
 * @property {string|null} resolutionNote
 */

/**
 * Returns the disputes inbox for the current user, scoped by their role:
 * - Team Member   → their own flagged/disputed responses
 * - TL+ OPERATIONS → direct reports' responses in their client(s)
 * - QA             → responses disputed in sessions they (or their subordinates) audited
 *
 * @returns {Promise<DisputeInboxRowDTO[]>}
 */
export const getDisputeInbox = () => api.get("/disputes/inbox");

export const getDisputeReasons = () => api.get("/dispute-reasons");

export const flagResponse = (responseId) =>
  api.post(`/disputes/flag/${responseId}`, undefined);

export const unflagResponse = (responseId) =>
  api.delete(`/disputes/flag/${responseId}`);

export const raiseDispute = (dto) => api.post("/disputes/raise", dto);

export const resolveDispute = (disputeId, dto) =>
  api.put(`/disputes/resolve/${disputeId}`, dto);