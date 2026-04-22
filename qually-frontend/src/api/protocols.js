/** @module api/protocols */

import { api } from "./apiClient";

/**
 * @typedef {Object} AuditProtocolRequestDTO
 * @property {string}                    protocolName
 * @property {number}                    protocolVersion
 * @property {"DRAFT"|"FINALIZED"|"ARCHIVED"} [protocolStatus]
 * @property {number}                    clientId
 * @property {"STANDARD"|"ACCOUNTABILITY"} auditLogicType - Required. Scoring strategy for all sessions.
 * @property {import('./questions').AuditQuestionRequestDTO[]} [auditQuestions]
 */

/**
 * @typedef {Object} AuditProtocolResponseDTO
 * @property {number}                    protocolId
 * @property {string}                    protocolName
 * @property {number}                    protocolVersion
 * @property {"DRAFT"|"FINALIZED"|"ARCHIVED"} protocolStatus
 * @property {"STANDARD"|"ACCOUNTABILITY"} auditLogicType - Scoring strategy for all sessions of this protocol.
 * @property {number}                    clientId
 * @property {string}                    clientName
 * @property {import('./questions').AuditQuestionResponseDTO[]} auditQuestions
 */

/**
 * Fetches all protocols, optionally filtered by client.
 *
 * @param {number} [clientId]
 * @returns {Promise<AuditProtocolResponseDTO[]>}
 */
export const getProtocols = (clientId) =>
  api.get(clientId ? `/protocols?clientId=${clientId}` : "/protocols");

/**
 * Fetches a single protocol by ID, including all nested questions and subattributes.
 *
 * @param {number|string} id
 * @returns {Promise<AuditProtocolResponseDTO>}
 */
export const getProtocolById = (id) =>
  api.get(`/protocols/${id}`);

/**
 * Creates a new protocol (deep-save: protocol + questions + subattributes + options).
 *
 * @param {AuditProtocolRequestDTO} data
 * @returns {Promise<AuditProtocolResponseDTO>}
 */
export const createProtocol = (data) =>
  api.post("/protocols", data);

/**
 * Replaces an existing protocol's data in full.
 *
 * @param {number|string} id
 * @param {AuditProtocolRequestDTO} data
 * @returns {Promise<AuditProtocolResponseDTO>}
 */
export const updateProtocol = (id, data) =>
  api.put(`/protocols/${id}`, data);

/**
 * Transitions a protocol from DRAFT → FINALIZED.
 *
 * @param {number|string} protocolId
 * @returns {Promise<AuditProtocolResponseDTO>}
 */
export const finalizeProtocol = (protocolId) =>
  api.patch(`/protocols/${protocolId}/finalize`);

/**
 * Updates only the protocol name (DRAFT only).
 *
 * @param {number|string} id
 * @param {string}        newName
 * @returns {Promise<AuditProtocolResponseDTO>}
 */
export const updateProtocolName = (id, newName) =>
  api.patch(`/protocols/${id}/name`, { protocolName: newName });
