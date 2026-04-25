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
 * @property {string} [newAnswer] - Required when MODIFIED: "YES", "NO", or "N/A".
 * @property {string} [resolutionNote]
 */

/**
 * @typedef {Object} AuditDisputeResponseDTO
 * @property {number} disputeId
 * @property {number} responseId
 * @property {number} raisedByUserId
 * @property {string} raisedByName
 * @property {number} reasonId
 * @property {string} reasonText
 * @property {string} [disputeComment]
 * @property {string} raisedAt
 * @property {number} [resolvedByUserId]
 * @property {string} [resolvedByName]
 * @property {string} [resolutionDate]
 * @property {string} [resolutionNote]
 * @property {"UNCHANGED"|"MODIFIED"|null} resolutionOutcome
 * @property {string} [newAnswer]
 */

/**
 * @typedef {Object} DisputeReason
 * @property {number} reasonId
 * @property {string} reasonText
 */

/** Fetches all predefined dispute reasons. */
export const getDisputeReasons = () => api.get("/dispute-reasons");

/** Flags a response as needing review. */
export const flagResponse = (responseId) =>
  api.post(`/disputes/flag/${responseId}`, undefined);

/** Removes a flag from a response with no paper trail. */
export const unflagResponse = (responseId) =>
  api.delete(`/disputes/flag/${responseId}`);

/**
 * Formally raises a dispute (Team Leader or above).
 * @param {AuditDisputeRequestDTO} dto
 * @returns {Promise<AuditDisputeResponseDTO>}
 */
export const raiseDispute = (dto) => api.post("/disputes/raise", dto);

/**
 * Resolves a dispute (QA only).
 * @param {number} disputeId
 * @param {ResolveDisputeRequestDTO} dto
 * @returns {Promise<AuditDisputeResponseDTO>}
 */
export const resolveDispute = (disputeId, dto) =>
  api.put(`/disputes/resolve/${disputeId}`, dto);
